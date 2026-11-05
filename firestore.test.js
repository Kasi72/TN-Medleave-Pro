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
