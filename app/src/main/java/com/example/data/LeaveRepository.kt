package com.example.data

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import com.google.firebase.Timestamp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.UUID

data class ExpiredGhClearResult(
  val deletedCount: Int,
  val failedCount: Int,
  val skippedInvalidCount: Int,
  val queuedOffline: Boolean
)

class LeaveRepository(private val db: FirebaseFirestore) {

  constructor(context: Context) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    )
  )

  private val auth = Firebase.auth

  companion object {
    const val BIOMETRIC_HOD_USER_ID = "biometric_hod_drkasi"
    private val VALID_FIRESTORE_DOC_ID_REGEX = Regex("^[a-zA-Z0-9_\\-]{1,128}$")
    internal fun isValidFirestoreDocId(id: String): Boolean = VALID_FIRESTORE_DOC_ID_REGEX.matches(id.trim())
    private const val BATCH_DELETE_CHUNK_SIZE = 200
    private const val BATCH_COMMIT_TIMEOUT_MS = 12_000L
    private const val SINGLE_DELETE_TIMEOUT_MS = 4_000L
    private val biometricSessionRecordsFlow = MutableStateFlow<List<LeaveRecord>>(emptyList())
    private val biometricWorkflowsFlow = MutableStateFlow<Map<String, LeaveWorkflowMeta>>(emptyMap())
    private val biometricDutyShiftsFlow = MutableStateFlow<List<DutyShift>>(emptyList())

    private fun ensureBiometricFallbackSeeded(): Int {
      if (biometricSessionRecordsFlow.value.isNotEmpty()) return 0
      val seeds = InitialLeaveSeedData.getAllHistoricalSeeds()
      val now = Timestamp.now()
      val initialList = seeds.mapIndexed { index, seed ->
        LeaveRecord(
          id = "seed_${seed.year}_${seed.leaveType.lowercase()}_${seed.dateIso}_$index",
          userId = BIOMETRIC_HOD_USER_ID,
          leaveType = seed.leaveType,
          dateIso = seed.dateIso,
          year = seed.year,
          pairedDateIso = seed.pairedDateIso,
          notes = seed.notes,
          highlightStatus = seed.highlightStatus,
          isHalfDay = seed.isHalfDay,
          createdAt = now,
          updatedAt = now
        )
      }
      biometricSessionRecordsFlow.value = initialList
      return initialList.size
    }
  }

  private fun requireUserId(): String {
    return auth.currentUser?.uid ?: BIOMETRIC_HOD_USER_ID
  }

  // ===========================================================================
  // LeaveEntry CRUD Operations (/users/{userId}/leave_entries/{entryId})
  // ===========================================================================

  /**
   * Real-time Flow of user `LeaveEntry` documents (with startDate, endDate, leaveType, status).
   */
  fun observeLeaveEntries(userId: String = requireUserId()): Flow<List<LeaveEntry>> {
    val path = "users/$userId/leave_entries"
    return db.collection("users")
      .document(userId)
      .collection("leave_entries")
      .whereEqualTo("userId", userId)
      .snapshots()
      .map { snapshot ->
        snapshot.toObjects(
          LeaveEntry::class.java,
          DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
        ).sortedByDescending { it.startDate }
      }
      .catch { error ->
        if (error is Exception) {
          handleFirestoreError(error, OperationType.LIST, path)
        }
        throw error
      }
  }

  /**
   * CREATE: Creates a new `LeaveEntry` in Firestore with start/end dates, leaveType, and status.
   */
  suspend fun createLeaveEntry(entry: LeaveEntry): Result<String> {
    val uid = requireUserId()
    val colRef = db.collection("users").document(uid).collection("leave_entries")
    val docRef = if (entry.id.isNotBlank()) colRef.document(entry.id) else colRef.document()
    val computedDays = if (entry.isHalfDay) {
      0.5
    } else {
      calculateInclusiveDays(entry.startDate, entry.endDate)
    }
    val derivedYear = entry.startDate.take(4).toLongOrNull() ?: entry.year
    val normalizedEntry = entry.copy(
      userId = uid,
      totalDays = computedDays,
      year = derivedYear
    )
    val payload = normalizedEntry.toWritePayload(authenticatedUid = uid, isUpdate = false)

    return try {
      docRef.set(payload).await()
      Result.success(docRef.id)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      Result.failure(e)
    }
  }

  /**
   * READ (Single): Fetches a single `LeaveEntry` by its document ID.
   */
  suspend fun getLeaveEntryById(entryId: String, targetUserId: String = requireUserId()): Result<LeaveEntry?> {
    val docRef = db.collection("users")
      .document(targetUserId)
      .collection("leave_entries")
      .document(entryId)
    return try {
      val snapshot = docRef.get().await()
      Result.success(
        snapshot.toObject(
          LeaveEntry::class.java,
          DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
        )
      )
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.GET, docRef.path)
      Result.failure(e)
    }
  }

  /**
   * READ (List): Fetches all `LeaveEntry` records for the authenticated user.
   */
  suspend fun getLeaveEntriesOnce(userId: String = requireUserId()): Result<List<LeaveEntry>> {
    val path = "users/$userId/leave_entries"
    return try {
      val snapshot = db.collection("users")
        .document(userId)
        .collection("leave_entries")
        .whereEqualTo("userId", userId)
        .get()
        .await()
      val list = snapshot.toObjects(
        LeaveEntry::class.java,
        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
      ).sortedByDescending { it.startDate }
      Result.success(list)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.LIST, path)
      Result.failure(e)
    }
  }

  /**
   * UPDATE: Updates an existing `LeaveEntry` document while preserving `createdAt`.
   */
  suspend fun updateLeaveEntry(entry: LeaveEntry): Result<Unit> {
    val uid = requireUserId()
    require(entry.id.isNotBlank()) { "LeaveEntry ID must not be blank for update" }
    requireNotNull(entry.createdAt) { "LeaveEntry createdAt must not be null for update" }

    val docRef = db.collection("users")
      .document(uid)
      .collection("leave_entries")
      .document(entry.id)
    val computedDays = if (entry.isHalfDay) {
      0.5
    } else {
      calculateInclusiveDays(entry.startDate, entry.endDate)
    }
    val derivedYear = entry.startDate.take(4).toLongOrNull() ?: entry.year
    val updated = entry.copy(
      userId = uid,
      totalDays = computedDays,
      year = derivedYear
    )
    val payload = updated.toWritePayload(authenticatedUid = uid, isUpdate = true)

    return try {
      docRef.set(payload).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      Result.failure(e)
    }
  }

  /**
   * DELETE: Deletes a `LeaveEntry` document by ID.
   */
  suspend fun deleteLeaveEntry(entryId: String): Result<Unit> {
    val uid = requireUserId()
    val docRef = db.collection("users")
      .document(uid)
      .collection("leave_entries")
      .document(entryId)
    return try {
      docRef.delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      Result.failure(e)
    }
  }

  // ===========================================================================
  // LeaveRecord CRUD Operations (/users/{userId}/leave_records/{recordId})
  // ===========================================================================

  fun observeLeaveRecords(userId: String = requireUserId()): Flow<List<LeaveRecord>> {
    if (auth.currentUser == null || userId == BIOMETRIC_HOD_USER_ID) {
      ensureBiometricFallbackSeeded()
      return biometricSessionRecordsFlow.map { list ->
        list.sortedByDescending { it.dateIso }
      }
    }
    val path = "users/$userId/leave_records"
    return db.collection("users")
      .document(userId)
      .collection("leave_records")
      .whereEqualTo("userId", userId)
      .snapshots()
      .map { snapshot ->
        snapshot.toObjects(
          LeaveRecord::class.java,
          DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
        ).sortedByDescending { it.dateIso }
      }
      .catch { error ->
        if (error is Exception) {
          handleFirestoreError(error, OperationType.LIST, path)
        }
        throw error
      }
  }

  suspend fun getLeaveRecordsOnce(userId: String = requireUserId()): Result<List<LeaveRecord>> {
    if (auth.currentUser == null || userId == BIOMETRIC_HOD_USER_ID) {
      ensureBiometricFallbackSeeded()
      return Result.success(biometricSessionRecordsFlow.value)
    }
    val path = "users/$userId/leave_records"
    return try {
      val snapshot = db.collection("users")
        .document(userId)
        .collection("leave_records")
        .whereEqualTo("userId", userId)
        .get()
        .await()
      val list = snapshot.toObjects(
        LeaveRecord::class.java,
        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
      )
      Result.success(list)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.LIST, path)
      Result.failure(e)
    }
  }

  suspend fun getLeaveRecordById(recordId: String, targetUserId: String = requireUserId()): Result<LeaveRecord?> {
    if (auth.currentUser == null || targetUserId == BIOMETRIC_HOD_USER_ID) {
      return Result.success(biometricSessionRecordsFlow.value.firstOrNull { it.id == recordId })
    }
    val docRef = db.collection("users")
      .document(targetUserId)
      .collection("leave_records")
      .document(recordId)
    return try {
      val snap = docRef.get().await()
      Result.success(snap.toObject(LeaveRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE))
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.GET, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun addLeaveRecord(
    leaveType: String,
    dateIso: String,
    year: Long,
    pairedDateIso: String = "",
    notes: String = "",
    highlightStatus: String = "NORMAL",
    isHalfDay: Boolean = false
  ): Result<String> {
    val uid = requireUserId()
    val proposedCat = LeaveCategory.fromCode(leaveType)
    val cleanDate = dateIso.trim()

    if (auth.currentUser == null || uid == BIOMETRIC_HOD_USER_ID) {
      ensureBiometricFallbackSeeded()
      val eval = com.example.domain.SmartDuplicateLeaveGuardEngine.evaluateProposedEntry(
        proposedCategory = proposedCat,
        candidateDatesIso = listOf(cleanDate),
        pairedDutyDateIso = pairedDateIso,
        proposedNotes = notes,
        isHalfDay = isHalfDay,
        existingRecords = biometricSessionRecordsFlow.value
      )
      if (!eval.canProceed) {
        return Result.failure(IllegalStateException(eval.detailedExplanation))
      }

      val now = Timestamp.now()
      val newId = "rec_${UUID.randomUUID().toString().take(8)}"
      val newRec = LeaveRecord(
        id = newId,
        userId = uid,
        leaveType = leaveType,
        dateIso = cleanDate,
        year = year,
        pairedDateIso = pairedDateIso,
        notes = notes,
        highlightStatus = highlightStatus,
        isHalfDay = isHalfDay,
        createdAt = now,
        updatedAt = now
      )
      biometricSessionRecordsFlow.value = biometricSessionRecordsFlow.value + newRec
      return Result.success(newId)
    }

    val colRef = db.collection("users").document(uid).collection("leave_records")
    return try {
      // Query same-date or same-paired-duty records to block duplicates atomically at repository level
      val sameDateSnap = colRef
        .whereEqualTo("userId", uid)
        .whereEqualTo("dateIso", cleanDate)
        .get()
        .await()
      val sameDateRecords = sameDateSnap.toObjects(
        LeaveRecord::class.java,
        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
      )
      val pairedConflictRecords = if (proposedCat == LeaveCategory.COL && pairedDateIso.isNotBlank()) {
        val pairedSnap = colRef
          .whereEqualTo("userId", uid)
          .whereEqualTo("leaveType", "COL")
          .whereEqualTo("pairedDateIso", pairedDateIso.trim())
          .get()
          .await()
        pairedSnap.toObjects(
          LeaveRecord::class.java,
          DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
        )
      } else {
        emptyList()
      }

      val eval = com.example.domain.SmartDuplicateLeaveGuardEngine.evaluateProposedEntry(
        proposedCategory = proposedCat,
        candidateDatesIso = listOf(cleanDate),
        pairedDutyDateIso = pairedDateIso,
        proposedNotes = notes,
        isHalfDay = isHalfDay,
        existingRecords = (sameDateRecords + pairedConflictRecords).distinctBy { it.id }
      )
      if (!eval.canProceed) {
        return Result.failure(IllegalStateException(eval.detailedExplanation))
      }

      val docRef = colRef.document()
      val payload = mapOf(
        "userId" to uid,
        "leaveType" to leaveType,
        "dateIso" to cleanDate,
        "year" to year,
        "pairedDateIso" to pairedDateIso,
        "notes" to notes,
        "highlightStatus" to highlightStatus,
        "isHalfDay" to isHalfDay,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
      )
      docRef.set(payload).await()
      Result.success(docRef.id)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, colRef.path)
      Result.failure(e)
    }
  }

  /**
   * Logs a single or multi-day `LeaveEntry` (with startDate, endDate, and status) into
   * `leave_entries` AND expands each day into `leave_records` so both the spell tracker and
   * day-by-day spreadsheet ledger stay in sync.
   */
  suspend fun addLeaveEntryWithDailyRecords(entry: LeaveEntry): Result<String> {
    return try {
      val fmt = DateTimeFormatter.ISO_LOCAL_DATE
      val start = LocalDate.parse(entry.startDate, fmt)
      val end = LocalDate.parse(entry.endDate, fmt)
      val mappedHighlight = when (entry.statusEnum) {
        LeaveStatus.VERIFIED_PAIRED -> "VERIFIED_GREEN"
        LeaveStatus.UNPAIRED_ALERT -> "ALERT_RED"
        LeaveStatus.PENDING -> "PENDING_ORANGE"
        else -> "NORMAL"
      }

      var curr = start
      var safetyCounter = 0
      var addedCount = 0
      var skippedDuplicateCount = 0
      var firstErrorMessage = "Duplicate leave entry rejected."

      while (!curr.isAfter(end) && safetyCounter < 31) {
        val iso = curr.format(fmt)
        val dayRes = addLeaveRecord(
          leaveType = entry.leaveType,
          dateIso = iso,
          year = curr.year.toLong(),
          pairedDateIso = entry.pairedDutyDate,
          notes = entry.reason,
          highlightStatus = mappedHighlight,
          isHalfDay = entry.isHalfDay && start == end
        )
        dayRes.onSuccess {
          addedCount++
        }.onFailure { err ->
          skippedDuplicateCount++
          if (firstErrorMessage == "Duplicate leave entry rejected.") {
            firstErrorMessage = err.localizedMessage ?: firstErrorMessage
          }
        }
        curr = curr.plusDays(1)
        safetyCounter++
      }

      if (addedCount == 0) {
        return Result.failure(IllegalStateException(firstErrorMessage))
      }

      if (auth.currentUser != null) {
        createLeaveEntry(entry)
      }
      val summaryTag = if (skippedDuplicateCount > 0) {
        "ADDED_${addedCount}_SKIPPED_${skippedDuplicateCount}"
      } else {
        "ADDED_${addedCount}"
      }
      Result.success(summaryTag)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun updateLeaveRecord(record: LeaveRecord): Result<Unit> {
    val uid = requireUserId()
    if (auth.currentUser == null || uid == BIOMETRIC_HOD_USER_ID) {
      val now = Timestamp.now()
      biometricSessionRecordsFlow.value = biometricSessionRecordsFlow.value.map { existing ->
        if (existing.id == record.id) record.copy(updatedAt = now) else existing
      }
      return Result.success(Unit)
    }
    require(record.id.isNotBlank()) { "Record ID must not be blank for update" }
    requireNotNull(record.createdAt) { "Record createdAt must not be null for update" }
    val docRef = db.collection("users").document(uid).collection("leave_records").document(record.id)
    val payload = mapOf(
      "userId" to uid,
      "leaveType" to record.leaveType,
      "dateIso" to record.dateIso,
      "year" to record.year,
      "pairedDateIso" to record.pairedDateIso,
      "notes" to record.notes,
      "highlightStatus" to record.highlightStatus,
      "isHalfDay" to record.isHalfDay,
      "createdAt" to record.createdAt,
      "updatedAt" to FieldValue.serverTimestamp()
    )
    return try {
      docRef.set(payload).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun deleteLeaveRecord(recordId: String): Result<Unit> {
    val uid = requireUserId()
    val cleanId = recordId.trim()
    if (!VALID_FIRESTORE_DOC_ID_REGEX.matches(cleanId)) {
      return Result.failure(IllegalArgumentException("Invalid record ID for deletion"))
    }
    if (auth.currentUser == null || uid == BIOMETRIC_HOD_USER_ID) {
      biometricSessionRecordsFlow.value = biometricSessionRecordsFlow.value.filterNot { it.id == cleanId }
      return Result.success(Unit)
    }
    val docRef = db.collection("users").document(uid).collection("leave_records").document(cleanId)
    return try {
      docRef.delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      Result.failure(e)
    }
  }

  /**
   * Resilient bulk-delete service for expired (>6-month) unclaimed GH_WORKED records.
   *
   * Safeguards against hangs and partial failures:
   * 1. Pre-validates every userId/recordId against `firestore.rules`'s `isValidId` regex so a single
   *    malformed ID can never poison an entire WriteBatch with PERMISSION_DENIED.
   * 2. Chunks deletes into batches of 200 (<=400 ops including optional workflow cleanup, safely under
   *    Firestore's 500-op WriteBatch limit).
   * 3. Guards each Firestore commit with a bounded timeout (`withTimeoutOrNull`) so offline/flaky
   *    hospital Wi-Fi never hangs the UI; Firestore's local cache applies deletes immediately and
   *    syncs automatically upon reconnection.
   * 4. Falls back to safe per-document iteration if any batch fails, isolating errors without
   *    blocking deletion of the remaining expired records.
   */
  suspend fun deleteExpiredGhWorkedRecords(
    recordIds: Collection<String>,
    workflowIdsToCleanup: Set<String> = emptySet()
  ): Result<ExpiredGhClearResult> {
    val uid = requireUserId()
    val distinctRaw = recordIds.map { it.trim() }.distinct()
    val validIds = distinctRaw.filter { VALID_FIRESTORE_DOC_ID_REGEX.matches(it) }
    val skippedInvalidCount = distinctRaw.size - validIds.size

    if (validIds.isEmpty()) {
      return Result.success(
        ExpiredGhClearResult(
          deletedCount = 0,
          failedCount = 0,
          skippedInvalidCount = skippedInvalidCount,
          queuedOffline = false
        )
      )
    }

    if (auth.currentUser == null || uid == BIOMETRIC_HOD_USER_ID) {
      val idSet = validIds.toSet()
      val before = biometricSessionRecordsFlow.value
      val after = before.filterNot { it.id in idSet }
      biometricSessionRecordsFlow.value = after
      if (workflowIdsToCleanup.isNotEmpty()) {
        biometricWorkflowsFlow.value = biometricWorkflowsFlow.value - idSet
      }
      return Result.success(
        ExpiredGhClearResult(
          deletedCount = (before.size - after.size).coerceAtLeast(0),
          failedCount = 0,
          skippedInvalidCount = skippedInvalidCount,
          queuedOffline = false
        )
      )
    }

    if (!VALID_FIRESTORE_DOC_ID_REGEX.matches(uid)) {
      return Result.failure(IllegalStateException("Invalid authenticated user ID for Firestore deletion"))
    }

    val userDoc = db.collection("users").document(uid)
    val recordsCol = userDoc.collection("leave_records")
    val workflowsCol = userDoc.collection("leave_workflows")
    val recordsPath = "users/$uid/leave_records"

    var deletedCount = 0
    var failedCount = 0
    var queuedOffline = false
    var lastError: Exception? = null

    for (chunk in validIds.chunked(BATCH_DELETE_CHUNK_SIZE)) {
      val batch = db.batch()
      chunk.forEach { recId ->
        batch.delete(recordsCol.document(recId))
        if (recId in workflowIdsToCleanup) {
          batch.delete(workflowsCol.document(recId))
        }
      }

      val batchSucceeded = try {
        val committed = withTimeoutOrNull(BATCH_COMMIT_TIMEOUT_MS) {
          batch.commit().await()
          true
        }
        if (committed == null) {
          // Offline / slow network: Firestore local persistence already removed the docs and queued sync
          queuedOffline = true
        }
        deletedCount += chunk.size
        true
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        lastError = e
        handleFirestoreError(e, OperationType.DELETE, recordsPath)
        false
      }

      // Per-document fallback if the batch failed so one bad document never blocks clearing the rest
      if (!batchSucceeded) {
        for (recId in chunk) {
          val docRef = recordsCol.document(recId)
          try {
            val done = withTimeoutOrNull(SINGLE_DELETE_TIMEOUT_MS) {
              docRef.delete().await()
              true
            }
            if (done == null) {
              queuedOffline = true
            }
            if (recId in workflowIdsToCleanup) {
              try {
                withTimeoutOrNull(SINGLE_DELETE_TIMEOUT_MS) {
                  workflowsCol.document(recId).delete().await()
                }
              } catch (e: CancellationException) {
                throw e
              } catch (_: Exception) {
                // Best-effort workflow metadata cleanup; primary record deletion already succeeded
              }
            }
            deletedCount++
          } catch (e: CancellationException) {
            throw e
          } catch (e: Exception) {
            lastError = e
            failedCount++
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
          }
        }
      }
    }

    return if (deletedCount == 0 && failedCount > 0 && lastError != null) {
      Result.failure(lastError)
    } else {
      Result.success(
        ExpiredGhClearResult(
          deletedCount = deletedCount,
          failedCount = failedCount,
          skippedInvalidCount = skippedInvalidCount,
          queuedOffline = queuedOffline
        )
      )
    }
  }

  suspend fun seedHistoricalDataIfEmpty(userId: String = requireUserId()): Result<Int> {
    if (auth.currentUser == null || userId == BIOMETRIC_HOD_USER_ID) {
      val count = ensureBiometricFallbackSeeded()
      return Result.success(count)
    }
    val colRef = db.collection("users").document(userId).collection("leave_records")
    return try {
      val existing = colRef.whereEqualTo("userId", userId).limit(1).get().await()
      if (!existing.isEmpty) {
        return Result.success(0)
      }
      val seeds = InitialLeaveSeedData.getAllHistoricalSeeds()
      var seededCount = 0
      val batch = db.batch()
      seeds.forEachIndexed { index, seed ->
        val docId = "seed_${seed.year}_${seed.leaveType.lowercase()}_${seed.dateIso}_$index"
        val docRef = colRef.document(docId)
        val payload = mapOf(
          "userId" to userId,
          "leaveType" to seed.leaveType,
          "dateIso" to seed.dateIso,
          "year" to seed.year,
          "pairedDateIso" to seed.pairedDateIso,
          "notes" to seed.notes,
          "highlightStatus" to seed.highlightStatus,
          "isHalfDay" to seed.isHalfDay,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp()
        )
        batch.set(docRef, payload)
        seededCount++
      }
      batch.commit().await()
      Result.success(seededCount)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, colRef.path)
      Result.failure(e)
    }
  }

  // ===========================================================================
  // LeaveWorkflowMeta CRUD (/users/{userId}/leave_workflows/{recordId})
  // ===========================================================================

  fun observeLeaveWorkflows(userId: String = requireUserId()): Flow<Map<String, LeaveWorkflowMeta>> {
    if (auth.currentUser == null || userId == BIOMETRIC_HOD_USER_ID) {
      return biometricWorkflowsFlow
    }
    val path = "users/$userId/leave_workflows"
    return db.collection("users")
      .document(userId)
      .collection("leave_workflows")
      .whereEqualTo("userId", userId)
      .snapshots()
      .map { snapshot ->
        snapshot.toObjects(
          LeaveWorkflowMeta::class.java,
          DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
        ).associateBy { it.recordId.ifBlank { it.id } }
      }
      .catch { error ->
        if (error is Exception) {
          handleFirestoreError(error, OperationType.LIST, path)
        }
        emit(emptyMap())
      }
  }

  suspend fun upsertLeaveWorkflow(meta: LeaveWorkflowMeta): Result<Unit> {
    val uid = requireUserId()
    val safeRecordId = meta.recordId.trim().take(128)
    if (safeRecordId.isBlank()) {
      return Result.failure(IllegalArgumentException("Record ID is required for workflow update"))
    }
    if (auth.currentUser == null || uid == BIOMETRIC_HOD_USER_ID) {
      val now = Timestamp.now()
      val updated = meta.copy(
        id = safeRecordId,
        userId = uid,
        recordId = safeRecordId,
        createdAt = meta.createdAt ?: now,
        updatedAt = now
      )
      biometricWorkflowsFlow.value = biometricWorkflowsFlow.value + (safeRecordId to updated)
      return Result.success(Unit)
    }
    val docRef = db.collection("users")
      .document(uid)
      .collection("leave_workflows")
      .document(safeRecordId)
    return try {
      val existing = docRef.get().await()
      if (existing.exists()) {
        docRef.update(meta.copy(recordId = safeRecordId).toUpdatePayload()).await()
      } else {
        docRef.set(meta.copy(recordId = safeRecordId).toCreatePayload(uid)).await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path)
      Result.failure(e)
    }
  }

  // ===========================================================================
  // Clinical DutyShift CRUD (/users/{userId}/duty_shifts/{shiftId})
  // ===========================================================================

  fun observeDutyShifts(userId: String = requireUserId()): Flow<List<DutyShift>> {
    if (auth.currentUser == null || userId == BIOMETRIC_HOD_USER_ID) {
      return biometricDutyShiftsFlow.map { list -> list.sortedBy { it.dateIso } }
    }
    val path = "users/$userId/duty_shifts"
    return db.collection("users")
      .document(userId)
      .collection("duty_shifts")
      .whereEqualTo("userId", userId)
      .snapshots()
      .map { snapshot ->
        snapshot.toObjects(
          DutyShift::class.java,
          DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
        ).sortedBy { it.dateIso }
      }
      .catch { error ->
        if (error is Exception) {
          handleFirestoreError(error, OperationType.LIST, path)
        }
        emit(emptyList())
      }
  }

  suspend fun addDutyShift(
    dateIso: String,
    shiftType: DutyShiftType,
    departmentNote: String = ""
  ): Result<String> {
    val uid = requireUserId()
    val cleanDate = dateIso.trim().take(10)
    val year = cleanDate.take(4).toLongOrNull()?.coerceIn(2020L, 2035L) ?: 2026L

    if (auth.currentUser == null || uid == BIOMETRIC_HOD_USER_ID) {
      val now = Timestamp.now()
      val newId = "shift_${cleanDate.replace("-", "")}_${shiftType.code.lowercase()}_${UUID.randomUUID().toString().take(4)}"
      val item = DutyShift(
        id = newId,
        userId = uid,
        dateIso = cleanDate,
        year = year,
        shiftType = shiftType.code,
        departmentNote = departmentNote.trim().take(200),
        colClaimed = false,
        linkedGhRecordId = "",
        createdAt = now,
        updatedAt = now
      )
      val filtered = biometricDutyShiftsFlow.value.filterNot {
        it.dateIso == cleanDate && it.shiftType == shiftType.code
      }
      biometricDutyShiftsFlow.value = filtered + item
      return Result.success(newId)
    }

    val colRef = db.collection("users").document(uid).collection("duty_shifts")
    val docId = "shift_${cleanDate.replace("-", "_")}_${shiftType.code.lowercase()}"
    val docRef = colRef.document(docId)
    return try {
      val existing = docRef.get().await()
      if (existing.exists()) {
        docRef.update(
          mapOf(
            "departmentNote" to departmentNote.trim().take(200),
            "updatedAt" to FieldValue.serverTimestamp()
          )
        ).await()
      } else {
        val shift = DutyShift(
          id = docId,
          userId = uid,
          dateIso = cleanDate,
          year = year,
          shiftType = shiftType.code,
          departmentNote = departmentNote.trim().take(200),
          colClaimed = false,
          linkedGhRecordId = ""
        )
        docRef.set(shift.toCreatePayload(uid)).await()
      }
      Result.success(docId)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun markDutyShiftColClaimed(shiftId: String, linkedGhRecordId: String): Result<Unit> {
    val uid = requireUserId()
    if (auth.currentUser == null || uid == BIOMETRIC_HOD_USER_ID) {
      val now = Timestamp.now()
      biometricDutyShiftsFlow.value = biometricDutyShiftsFlow.value.map { s ->
        if (s.id == shiftId) {
          s.copy(colClaimed = true, linkedGhRecordId = linkedGhRecordId, updatedAt = now)
        } else {
          s
        }
      }
      return Result.success(Unit)
    }
    val docRef = db.collection("users").document(uid).collection("duty_shifts").document(shiftId)
    return try {
      docRef.update(
        mapOf(
          "colClaimed" to true,
          "linkedGhRecordId" to linkedGhRecordId.trim().take(128),
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun deleteDutyShift(shiftId: String): Result<Unit> {
    val uid = requireUserId()
    if (auth.currentUser == null || uid == BIOMETRIC_HOD_USER_ID) {
      biometricDutyShiftsFlow.value = biometricDutyShiftsFlow.value.filterNot { it.id == shiftId }
      return Result.success(Unit)
    }
    val docRef = db.collection("users").document(uid).collection("duty_shifts").document(shiftId)
    return try {
      docRef.delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      Result.failure(e)
    }
  }

  private fun calculateInclusiveDays(startIso: String, endIso: String): Double {
    return try {
      val fmt = DateTimeFormatter.ISO_LOCAL_DATE
      val s = LocalDate.parse(startIso, fmt)
      val e = LocalDate.parse(endIso, fmt)
      val diff = ChronoUnit.DAYS.between(s, e) + 1
      diff.coerceAtLeast(1L).toDouble()
    } catch (e: Exception) {
      1.0
    }
  }
}
