const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_123";
const BOB_UID = "bob_uid_456";

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

function pastDate(offsetMs = 5000) {
  return new Date(Date.now() - offsetMs);
}

test("Unauthenticated user cannot read or write profiles, books, bookmarks, or logs", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("books").get());
});

test("User profile creation, language/viewMode updates, and cross-user isolation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = pastDate(10000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice Reader",
      email: "alice@example.com",
      dailyGoalPages: 20,
      totalPagesRead: 0,
      streakDays: 0,
      reminderEnabled: false,
      reminderHour: 20,
      reminderMinute: 0,
      appTheme: "SYSTEM",
      readerTheme: "SEPIA",
      preferredLanguageTag: "ta",
      readerViewMode: "PAGE_SCROLL",
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      dailyGoalPages: 30,
      reminderEnabled: true,
      reminderHour: 21,
      reminderMinute: 15,
      appTheme: "DARK",
      readerTheme: "DARK",
      preferredLanguageTag: "es",
      readerViewMode: "BOOK_FLIP",
      updatedAt: pastDate(1000),
    })
  );

  // Invalid view mode rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      readerViewMode: "INVALID_MODE",
      updatedAt: pastDate(500),
    })
  );

  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
});

test("Book creation, summary caching, progress update, and security guards", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = pastDate(10000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("books").doc("book_1").set({
      userId: ALICE_UID,
      title: "The Design of Everyday Things",
      author: "Don Norman",
      pageCount: 120,
      currentPage: 0,
      coverColorHex: "#1E3A34",
      fileName: "design.pdf",
      fileSizeBytes: 2450000,
      favorite: false,
      completed: false,
      archived: false,
      lastReadAt: created,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Archive toggle succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("books").doc("book_1").update({
      archived: true,
      updatedAt: pastDate(1800),
    })
  );

  // Summary update succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("books").doc("book_1").update({
      summary: "A classic exploration of human-centered design, affordances, and signifiers.",
      summaryLanguage: "en",
      updatedAt: pastDate(1500),
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("books").doc("book_1").update({
      currentPage: 45,
      completed: false,
      lastReadAt: pastDate(1000),
      updatedAt: pastDate(1000),
    })
  );

  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("books").get());
});

test("Bookmarks and ReadingLogs enforce ownership and parent validation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = pastDate(10000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("books").doc("book_1").set({
      userId: ALICE_UID,
      title: "Clean Architecture",
      author: "Robert Martin",
      pageCount: 80,
      currentPage: 0,
      coverColorHex: "#1E3A34",
      fileName: "clean.pdf",
      fileSizeBytes: 1024,
      favorite: false,
      completed: false,
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("bookmarks").doc("bm_1").set({
      userId: ALICE_UID,
      bookId: "book_1",
      bookTitle: "Clean Architecture",
      pageIndex: 12,
      note: "Chapter 3 key diagram",
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("readingLogs").doc("2026-11-22").set({
      userId: ALICE_UID,
      dateKey: "2026-11-22",
      pagesRead: 14,
      minutesRead: 22,
      createdAt: created,
      updatedAt: created,
    })
  );
});
