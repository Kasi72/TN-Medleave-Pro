const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user: cannot read leave records or leave entries", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("leave_records").get()
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("leave_entries").get()
  );
});

test("Authenticated user: cannot read another user's leave entry", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context
      .firestore()
      .collection("users")
      .doc(BOB_UID)
      .collection("leave_entries")
      .doc("entry_1")
      .set({
        userId: BOB_UID,
        leaveType: "EL",
        startDate: "2026-05-26",
        endDate: "2026-06-01",
        totalDays: 7.0,
        status: "APPROVED",
        year: 2026,
        pairedDutyDate: "",
        reason: "Annual Earned Leave",
        isHalfDay: false,
        createdAt: new Date(Date.now() - 10000),
        updatedAt: new Date(Date.now() - 10000),
      });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).collection("leave_entries").doc("entry_1").get()
  );
});

test("Authenticated user: can create, update, and read own valid LeaveEntry", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const docRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("leave_entries")
    .doc("entry_alice_1");

  const createdTime = new Date(Date.now() - 5000);
  await assertSucceeds(
    docRef.set({
      userId: ALICE_UID,
      leaveType: "COL",
      startDate: "2026-11-02",
      endDate: "2026-11-03",
      totalDays: 2.0,
      status: "VERIFIED_PAIRED",
      year: 2026,
      pairedDutyDate: "2026-04-03",
      reason: "COL paired with Good Friday GH Worked",
      isHalfDay: false,
      createdAt: createdTime,
      updatedAt: createdTime,
    })
  );

  await assertSucceeds(docRef.get());
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("leave_entries")
      .where("userId", "==", ALICE_UID)
      .get()
  );
});

test("Authenticated user: fails shadow update with ghost field on leave_entries", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const docRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("leave_entries")
    .doc("entry_alice_ghost");

  await assertFails(
    docRef.set({
      userId: ALICE_UID,
      leaveType: "CL",
      startDate: "2026-04-27",
      endDate: "2026-04-27",
      totalDays: 1.0,
      status: "APPROVED",
      year: 2026,
      pairedDutyDate: "",
      reason: "Test",
      isHalfDay: false,
      isAdmin: true,
      createdAt: new Date(Date.now() - 5000),
      updatedAt: new Date(Date.now() - 5000),
    })
  );
});

test("LeaveWorkflowMeta: owner can create, update, list, and delete; unauth & cross-user denied", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  const wfRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("leave_workflows")
    .doc("rec_101");

  const created = new Date(Date.now() - 5000);
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("leave_workflows").get()
  );

  await assertSucceeds(
    wfRef.set({
      userId: ALICE_UID,
      recordId: "rec_101",
      stage: "SUBMITTED",
      sanctionOrderNo: "Ref.No. 4182/E1/2026",
      relievingOfficer: "Dr. Aravind (Asst. Prof)",
      relievingContact: "9842200112",
      joiningDateIso: "",
      joiningSession: "FN",
      slipLocalFileName: "",
      workflowNotes: "Submitted to HOD Psychiatry",
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    wfRef.update({
      stage: "JOINING_FILED",
      joiningDateIso: "2026-12-02",
      joiningSession: "FN",
      slipLocalFileName: "slip_rec_101.jpg",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("leave_workflows").where("userId", "==", ALICE_UID).get()
  );

  // Cross-user read/write denied
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("leave_workflows").doc("rec_101").get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("leave_workflows").doc("rec_101").update({
      stage: "SANCTIONED",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Invalid stage & immutable createdAt mutation rejected
  await assertFails(
    wfRef.update({
      stage: "INVALID_STAGE",
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(
    wfRef.update({
      createdAt: new Date(Date.now() - 200),
      updatedAt: new Date(Date.now() - 200),
    })
  );
  await assertSucceeds(wfRef.delete());
});

test("DutyShift: owner can create, update colClaimed, query, and delete; cross-user & ghost fields denied", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const shiftRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("duty_shifts")
    .doc("shift_2026_12_25");

  const created = new Date(Date.now() - 5000);
  await assertSucceeds(
    shiftRef.set({
      userId: ALICE_UID,
      dateIso: "2026-12-25",
      year: 2026,
      shiftType: "GH_EMERGENCY",
      departmentNote: "Christmas Emergency Casualty Call",
      colClaimed: false,
      linkedGhRecordId: "",
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    shiftRef.update({
      colClaimed: true,
      linkedGhRecordId: "gh_rec_999",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("duty_shifts").where("userId", "==", ALICE_UID).get()
  );

  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("duty_shifts").doc("shift_2026_12_25").get()
  );

  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("duty_shifts").doc("shift_bad").set({
      userId: ALICE_UID,
      dateIso: "2026-12-26",
      year: 2026,
      shiftType: "UNKNOWN_DUTY",
      departmentNote: "",
      colClaimed: false,
      linkedGhRecordId: "",
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(shiftRef.delete());
});
