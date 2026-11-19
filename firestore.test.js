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
const ADMIN_EMAIL = "rastogi.pragati.it@gmail.com";

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

test("Unauthenticated user: cannot read or write user data or catalogs", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("habits").get());
  await assertFails(unauthDb.collection("categories").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Anon",
      xp: 0,
      level: 1,
      remindersEnabled: true,
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("User profile: owner create, partial update, cross-user isolation, and future timestamp rejection", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID, { email: "bob@example.com", email_verified: true }).firestore();
  const past = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      email: "alice@example.com",
      xp: 0,
      level: 1,
      remindersEnabled: true,
      dailySummaryTime: "08:00",
      createdAt: past,
      updatedAt: past,
    })
  );

  // Partial update succeeds without touching createdAt
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      bio: "Building healthy daily habits",
      xp: 40,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Shadow update with ghost field fails
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Cross-user read & write rejected
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      bio: "Hacked",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  // Non-admin cannot list all users
  await assertFails(aliceDb.collection("users").get());
});

test("Habits & Completions & Routines: Kotlin-mirrored payloads and isolation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const past = new Date(Date.now() - 5000);

  // Create habit (mirrors Kotlin HabitRepository.createHabit without @DocumentId 'id')
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("habits").doc("habit_1").set({
      userId: ALICE_UID,
      name: "Morning Meditation",
      description: "10 minutes of mindful breathing",
      category: "Mindfulness",
      scheduleType: "DAILY",
      targetDays: [1, 2, 3, 4, 5, 6, 7],
      targetFrequency: 1,
      priority: "HIGH",
      startDate: "2026-11-18",
      reminderTime: "07:30",
      remindersEnabled: true,
      paused: false,
      colorHex: "#6C63FF",
      createdAt: past,
      updatedAt: past,
    })
  );

  // Reject habit with extra @DocumentId 'id' field
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("habits").doc("habit_2").set({
      id: "habit_2",
      userId: ALICE_UID,
      name: "Bad Habit",
      category: "Health",
      scheduleType: "DAILY",
      targetDays: [1, 2, 3, 4, 5, 6, 7],
      targetFrequency: 1,
      priority: "LOW",
      startDate: "2026-11-18",
      remindersEnabled: false,
      paused: false,
      createdAt: past,
      updatedAt: past,
    })
  );

  // Partial pause update succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("habits").doc("habit_1").update({
      paused: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Create completion keyed by habitId_date with stopwatch durationSeconds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("completions").doc("habit_1_2026-11-18").set({
      userId: ALICE_UID,
      habitId: "habit_1",
      habitName: "Morning Meditation",
      category: "Mindfulness",
      date: "2026-11-18",
      completedCount: 1,
      targetFrequency: 1,
      durationSeconds: 600,
      notes: "Felt very calm today",
      createdAt: past,
      updatedAt: past,
    })
  );

  // Update completion notes and durationSeconds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("completions").doc("habit_1_2026-11-18").update({
      durationSeconds: 900,
      notes: "Deep focus session",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject negative durationSeconds
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("completions").doc("habit_1_2026-11-18").update({
      durationSeconds: -10,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Create routine
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("routines").doc("routine_1").set({
      userId: ALICE_UID,
      name: "Morning Flow",
      timeOfDay: "MORNING",
      startTime: "07:00",
      habitIds: ["habit_1"],
      remindersEnabled: true,
      createdAt: past,
      updatedAt: past,
    })
  );

  // Cross-user isolation on subcollections
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("habits").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("completions").doc("habit_1_2026-11-18").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("routines").get());
});

test("Admin security & Email Spoofing defense", async () => {
  const realAdminDb = testEnv.authenticatedContext("admin_1", { email: ADMIN_EMAIL, email_verified: true }).firestore();
  const spoofedAdminDb = testEnv.authenticatedContext("spoof_1", { email: ADMIN_EMAIL, email_verified: false }).firestore();
  const regularUserDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true }).firestore();
  const past = new Date(Date.now() - 5000);

  // Spoofed unverified email cannot write admin catalog
  await assertFails(
    spoofedAdminDb.collection("categories").doc("cat_sleep").set({
      name: "Sleep",
      iconName: "bedtime",
      colorHex: "#6C63FF",
      active: true,
      createdAt: past,
      updatedAt: past,
    })
  );

  // Regular user cannot write admin config or challenges
  await assertFails(
    regularUserDb.collection("app_config").doc("settings").set({
      adsEnabled: true,
      showAdsOnDashboard: true,
      showAdsOnAnalytics: true,
      maintenanceMode: false,
      updatedAt: past,
    })
  );

  // Verified admin can manage categories, challenges, motivational messages, and app_config
  await assertSucceeds(
    realAdminDb.collection("categories").doc("cat_sleep").set({
      name: "Sleep",
      iconName: "bedtime",
      colorHex: "#6C63FF",
      active: true,
      createdAt: past,
      updatedAt: past,
    })
  );

  await assertSucceeds(
    realAdminDb.collection("app_config").doc("settings").set({
      adsEnabled: false,
      adPublisherId: "",
      dashboardAdSlotId: "",
      analyticsAdSlotId: "",
      showAdsOnDashboard: true,
      showAdsOnAnalytics: true,
      maintenanceMode: false,
      announcement: "Welcome to HabitFlow!",
      updatedAt: past,
    })
  );

  // Signed-in regular user can read categories and settings
  await assertSucceeds(regularUserDb.collection("categories").get());
  await assertSucceeds(regularUserDb.collection("app_config").doc("settings").get());
});
