package com.example.data

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class LeaveRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun createLeaveEntry_validPayload_createsDocumentAndSupportsReadUpdateDelete() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repository = LeaveRepository(firestore)

    val entry = LeaveEntry(
      leaveType = "EL",
      startDate = "2026-05-26",
      endDate = "2026-06-01",
      totalDays = 7.0,
      status = LeaveStatus.APPROVED.code,
      year = 2026L,
      pairedDutyDate = "",
      reason = "Earned Leave Summer Spell"
    )

    val createResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      repository.createLeaveEntry(entry)
    }
    assertTrue(createResult.isSuccess)
    val entryId = createResult.getOrThrow()
    assertTrue(entryId.isNotEmpty())

    // Read back
    val fetched = withTimeout(DEFAULT_TIMEOUT_MS) {
      repository.getLeaveEntryById(entryId, uid).getOrThrow()
    }
    assertNotNull(fetched)
    assertEquals("2026-05-26", fetched?.startDate)
    assertEquals("2026-06-01", fetched?.endDate)
    assertEquals("APPROVED", fetched?.status)

    // Update status
    val updateRes = withTimeout(DEFAULT_TIMEOUT_MS) {
      repository.updateLeaveEntry(fetched!!.copy(status = LeaveStatus.VERIFIED_PAIRED.code))
    }
    assertTrue(updateRes.isSuccess)

    // Delete
    val deleteRes = withTimeout(DEFAULT_TIMEOUT_MS) {
      repository.deleteLeaveEntry(entryId)
    }
    assertTrue(deleteRes.isSuccess)
  }

  @Test
  fun addLeaveRecord_validPayload_createsDocumentAndReturnsId() = runBlocking {
    signInTestUser(ALICE_EMAIL)
    val repository = LeaveRepository(firestore)

    val createResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      repository.addLeaveRecord(
        leaveType = "CL",
        dateIso = "2026-11-23",
        year = 2026L,
        pairedDateIso = "",
        notes = "Karthigai Deepam Bridge Leave"
      )
    }
    assertTrue(createResult.isSuccess)
    val recordId = createResult.getOrThrow()
    assertTrue(recordId.isNotEmpty())
  }

  @Test
  fun observeLeaveRecords_authenticatedOwner_emitsRealtimeUpdates() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repository = LeaveRepository(firestore)
    val recordId = withTimeout(DEFAULT_TIMEOUT_MS) {
      repository.addLeaveRecord(
        leaveType = "RH",
        dateIso = "2026-11-24",
        year = 2026L,
        notes = "Karthigai Deepam RH"
      ).getOrThrow()
    }

    val emitted = withTimeout(FLOW_TIMEOUT_MS) {
      repository.observeLeaveRecords(uid).first { list -> list.any { it.id == recordId } }
    }
    assertTrue(emitted.any { it.id == recordId })
  }

  @Test
  fun getLeaveEntryById_crossUserAccess_failsWithPermissionDenied() = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    val aliceRepo = LeaveRepository(firestore)
    val entryId = withTimeout(DEFAULT_TIMEOUT_MS) {
      aliceRepo.createLeaveEntry(
        LeaveEntry(
          leaveType = "COL",
          startDate = "2026-11-02",
          endDate = "2026-11-03",
          status = LeaveStatus.VERIFIED_PAIRED.code,
          year = 2026L,
          pairedDutyDate = "2026-04-03",
          reason = "COL paired with Good Friday GH"
        )
      ).getOrThrow()
    }

    signInTestUser(BOB_EMAIL)
    val bobRepo = LeaveRepository(firestore)
    val result = withTimeout(DEFAULT_TIMEOUT_MS) {
      bobRepo.getLeaveEntryById(entryId, targetUserId = aliceUid)
    }
    assertTrue(result.isFailure)
    val firestoreEx = result.exceptionOrNull()?.unwrapFirestoreException()
    assertNotNull(firestoreEx)
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
  }

  @Test
  fun observeLeaveEntries_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    auth.signOut()
    val repository = LeaveRepository(firestore)

    try {
      withTimeout(FLOW_TIMEOUT_MS) {
        repository.observeLeaveEntries(aliceUid).first()
      }
      fail("Expected exception for unauthenticated flow")
    } catch (e: Exception) {
      val firestoreEx = checkNotNull(e.unwrapFirestoreException()) {
        "Expected nested FirebaseFirestoreException, got $e"
      }
      assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx.code)
    }
  }

  private companion object {
    const val ALICE_EMAIL = "hod.psychiatry@erodemed.edu.in"
    const val BOB_EMAIL = "other.doctor@erodemed.edu.in"
    const val DEFAULT_TIMEOUT_MS = 5000L
    const val FLOW_TIMEOUT_MS = 4000L
  }
}
