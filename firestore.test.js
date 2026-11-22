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
const CHARLIE_UID = "charlie_789";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
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

test("Unauthenticated users are denied across all collections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users_public").where("searchable", "==", true).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("conversations").where("participantUids", "array-contains", ALICE_UID).get());
  await assertFails(unauthDb.collection("calls").where("participantUids", "array-contains", ALICE_UID).get());
});

test("Public directory & Private user PII isolation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 2000);

  // Alice creates public searchable profile (NO PII)
  await assertSucceeds(
    aliceDb.collection("users_public").doc(ALICE_UID).set({
      uid: ALICE_UID,
      displayName: "Alice Rahman",
      username: "alice_r",
      bio: "Explorer",
      status: "Online",
      searchable: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Alice creates private account (with email & phone)
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice Rahman",
      username: "alice_r",
      email: "alice@example.com",
      phoneNumber: "+8801700000000",
      defaultSearchEngine: "Google",
      darkModeWeb: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Bob can query searchable public profiles, but CANNOT read Alice's private PII doc
  await assertSucceeds(
    bobDb.collection("users_public").where("searchable", "==", true).get()
  );
  await assertFails(bobDb.collection("users_public").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users_public").doc(ALICE_UID).update({ bio: "Hijacked", updatedAt: new Date(Date.now() - 500) })
  );
});

test("User-scoped bookmarks, history, and call_history", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 2000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("bookmarks").doc("bm_1").set({
      userId: ALICE_UID,
      title: "Wikipedia",
      url: "https://www.wikipedia.org",
      category: "Learning",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("bookmarks").get()
  );

  // Ghost field 'id' rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("bookmarks").doc("bm_2").set({
      id: "bm_2",
      userId: ALICE_UID,
      title: "Bad",
      url: "https://example.com",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Cross-user access denied
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("bookmarks").get()
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("history").doc("hist_1").set({
      userId: ALICE_UID,
      title: "BBC News",
      url: "https://www.bbc.com",
      visitedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("call_history").doc("ch_1").set({
      userId: ALICE_UID,
      peerName: "Bob",
      callType: "VIDEO",
      direction: "OUTGOING",
      status: "ENDED",
      durationSec: 120,
      createdAt: now,
    })
  );
});

test("Conversations & denormalized messages security", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const now = new Date(Date.now() - 2000);

  await assertSucceeds(
    aliceDb.collection("conversations").doc("conv_1").set({
      title: "Project Orbit",
      groupChat: true,
      participantUids: [ALICE_UID, BOB_UID],
      participantSummary: "Alice, Bob",
      createdBy: ALICE_UID,
      lastMessageText: "Hello team!",
      lastMessageType: "TEXT",
      lastSenderUid: ALICE_UID,
      lastMessageAt: now,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("conversations").doc("conv_1").collection("messages").doc("msg_1").set({
      conversationId: "conv_1",
      senderUid: ALICE_UID,
      senderName: "Alice",
      participantUids: [ALICE_UID, BOB_UID],
      messageType: "WEB_SHARE",
      text: "Check out this page",
      mediaUrl: "https://www.wikipedia.org",
      sharedTitle: "Wikipedia",
      readBy: [ALICE_UID],
      createdAt: now,
      updatedAt: now,
    })
  );

  // Bob (participant) can query filtered messages and mark seen
  await assertSucceeds(
    bobDb
      .collection("conversations")
      .doc("conv_1")
      .collection("messages")
      .where("participantUids", "array-contains", BOB_UID)
      .get()
  );

  await assertSucceeds(
    bobDb.collection("conversations").doc("conv_1").collection("messages").doc("msg_1").update({
      readBy: [ALICE_UID, BOB_UID],
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Charlie (non-participant) cannot read or write
  await assertFails(charlieDb.collection("conversations").doc("conv_1").get());
  await assertFails(
    charlieDb.collection("conversations").doc("conv_1").collection("messages").doc("msg_1").get()
  );
});

test("Voice/Video/Group calls: max 8 participants and terminal state lock", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const now = new Date(Date.now() - 2000);

  await assertSucceeds(
    aliceDb.collection("calls").doc("call_1").set({
      callerUid: ALICE_UID,
      callerName: "Alice",
      callType: "GROUP_VIDEO",
      status: "RINGING",
      participantUids: [ALICE_UID, BOB_UID],
      activeUids: [ALICE_UID],
      title: "Team Sync",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Bob answers
  await assertSucceeds(
    bobDb.collection("calls").doc("call_1").update({
      status: "ACTIVE",
      activeUids: [ALICE_UID, BOB_UID],
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Alice ends call
  await assertSucceeds(
    aliceDb.collection("calls").doc("call_1").update({
      status: "ENDED",
      activeUids: [],
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Terminal state lock: cannot reopen ENDED call
  await assertFails(
    bobDb.collection("calls").doc("call_1").update({
      status: "ACTIVE",
      updatedAt: new Date(Date.now() - 200),
    })
  );

  // Charlie cannot inspect call
  await assertFails(charlieDb.collection("calls").doc("call_1").get());

  // 9 participants rejected (max 8)
  await assertFails(
    aliceDb.collection("calls").doc("call_too_big").set({
      callerUid: ALICE_UID,
      callerName: "Alice",
      callType: "GROUP_VOICE",
      status: "RINGING",
      participantUids: [ALICE_UID, "u2", "u3", "u4", "u5", "u6", "u7", "u8", "u9"],
      activeUids: [ALICE_UID],
      title: "Too Many",
      createdAt: now,
      updatedAt: now,
    })
  );
});
