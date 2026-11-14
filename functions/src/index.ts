/**
 * AeroFast Pro licence backend.
 *
 * Trust model: the Android client is assumed to be hostile. Nothing here believes a
 * client-supplied claim except the device id, which is bounded by App Check (Play Integrity)
 * attesting that the call came from an unmodified build of this app on a genuine device.
 */
import { createHash, randomBytes } from "crypto";
import { initializeApp } from "firebase-admin/app";
import { getAuth } from "firebase-admin/auth";
import { getDatabase } from "firebase-admin/database";
import { HttpsError, onCall, CallableRequest } from "firebase-functions/v2/https";
import { onSchedule } from "firebase-functions/v2/scheduler";
import { setGlobalOptions } from "firebase-functions/v2";

initializeApp();
setGlobalOptions({ region: "asia-southeast1", maxInstances: 10 });

/** The single account allowed to mint, revoke and unbind licences. */
const ADMIN_EMAIL = "parandhama9999@gmail.com";

const db = () => getDatabase();

/**
 * The signed-in identity for a licence. Derived from a hash so the plaintext key never
 * appears in a token, a uid, or anywhere the security rules can read it.
 */
const uidFor = (licenseKey: string): string =>
  "lic_" + createHash("sha256").update(licenseKey).digest("hex").slice(0, 32);

/** AERO-XXXX-YYYY, from crypto-grade randomness, with ambiguous glyphs removed. */
function generateKey(): string {
  const alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // no I, O, 0, 1
  const bytes = randomBytes(8);
  const block = (from: number) =>
    Array.from(bytes.slice(from, from + 4), (b) => alphabet[b % alphabet.length]).join("");
  return `AERO-${block(0)}-${block(4)}`;
}

function normaliseKey(raw: unknown): string {
  if (typeof raw !== "string") throw new HttpsError("invalid-argument", "Licence key is required.");
  const key = raw.trim().toUpperCase().replace(/\s+/g, "");
  // Firebase path segments cannot contain . # $ [ ] /
  if (!/^AERO-[A-Z0-9]{4}-[A-Z0-9]{4}$/.test(key)) {
    throw new HttpsError("invalid-argument", "That is not a valid licence key format.");
  }
  return key;
}

function normaliseDeviceId(raw: unknown): string {
  if (typeof raw !== "string" || raw.trim().length < 8 || raw.trim().length > 128) {
    throw new HttpsError("invalid-argument", "A device id is required.");
  }
  return raw.trim();
}

/** Rejects the call unless App Check attested it, in production. */
function requireAppCheck(req: CallableRequest): void {
  if (process.env.FUNCTIONS_EMULATOR === "true") return;
  if (!req.app) {
    throw new HttpsError("failed-precondition", "This request did not come from the AeroFast Pro app.");
  }
}

function requireAdmin(req: CallableRequest): void {
  requireAppCheck(req);
  if (req.auth?.token?.admin !== true) {
    throw new HttpsError("permission-denied", "Administrator access is required.");
  }
}

interface LicenseRecord {
  isActive: boolean;
  boundDeviceId: string | null;
  expiryDate: number;
  createdAt: number;
  note?: string;
  lastSeenAt?: number;
  activatedAt?: number;
}

/**
 * Mirrors just the two facts the database rules need, under the uid the client signs in as.
 * The rules read this; they never see /licenses, so a licence key cannot be harvested from
 * a rule evaluation and the admin tree stays entirely private.
 */
async function writeStatusMirror(uid: string, rec: LicenseRecord): Promise<void> {
  await db().ref(`licenseStatus/${uid}`).set({
    active: rec.isActive === true && rec.expiryDate > Date.now(),
    expiry: rec.expiryDate,
  });
}

// ---------------------------------------------------------------------------------------------
// User app
// ---------------------------------------------------------------------------------------------

/**
 * Binds a licence to the calling device and returns a custom auth token.
 *
 * The bind is a transaction, so two devices racing the same key cannot both win: exactly one
 * observes boundDeviceId === null and claims it.
 */
export const activateLicense = onCall({ enforceAppCheck: true }, async (req) => {
  requireAppCheck(req);
  const licenseKey = normaliseKey(req.data?.licenseKey);
  const deviceId = normaliseDeviceId(req.data?.deviceId);
  const now = Date.now();

  const ref = db().ref(`licenses/${licenseKey}`);
  type Failure = "no-such-key" | "revoked" | "expired" | "bound-elsewhere" | null;
  let failure: Failure = null;

  const result = await ref.transaction((current: LicenseRecord | null) => {
    failure = null;
    if (current === null) {
      failure = "no-such-key";
      return; // abort
    }
    if (current.isActive !== true) {
      failure = "revoked";
      return;
    }
    if (current.expiryDate <= now) {
      failure = "expired";
      return;
    }
    if (current.boundDeviceId && current.boundDeviceId !== deviceId) {
      failure = "bound-elsewhere";
      return;
    }
    return {
      ...current,
      boundDeviceId: deviceId,
      activatedAt: current.activatedAt ?? now,
      lastSeenAt: now,
    };
  });

  // Read through a fresh binding: the assignments above happen inside the transaction closure,
  // which the compiler's control-flow analysis does not follow.
  switch (failure as Failure) {
    case "no-such-key":
      // Same wording as a revoked key, so the endpoint cannot be used to test whether a key exists.
      throw new HttpsError("not-found", "This licence key is not valid. Contact the administrator.");
    case "revoked":
      throw new HttpsError("not-found", "This licence key is not valid. Contact the administrator.");
    case "expired":
      throw new HttpsError("deadline-exceeded", "This licence has expired. Contact the administrator to renew it.");
    case "bound-elsewhere":
      throw new HttpsError(
        "already-exists",
        "This key is already bound to another device. Contact Admin."
      );
  }
  if (!result.committed || !result.snapshot.exists()) {
    throw new HttpsError("aborted", "Activation could not be completed. Please try again.");
  }

  const rec = result.snapshot.val() as LicenseRecord;
  const uid = uidFor(licenseKey);
  await writeStatusMirror(uid, rec);

  return {
    token: await getAuth().createCustomToken(uid, { lic: true }),
    expiryDate: rec.expiryDate,
  };
});

/**
 * Called on app open and once a day. Re-reads live state rather than trusting the token, so a
 * revoke or an unbind takes effect on the next heartbeat instead of waiting for token expiry.
 */
export const heartbeat = onCall({ enforceAppCheck: true }, async (req) => {
  requireAppCheck(req);
  if (!req.auth) throw new HttpsError("unauthenticated", "Not signed in.");
  const deviceId = normaliseDeviceId(req.data?.deviceId);
  const uid = req.auth.uid;

  const snap = await db().ref("licenses").orderByChild("boundDeviceId").equalTo(deviceId).get();
  let found: { key: string; rec: LicenseRecord } | null = null;
  snap.forEach((child) => {
    if (uidFor(child.key as string) === uid) found = { key: child.key as string, rec: child.val() };
    return false;
  });

  if (!found) {
    await db().ref(`licenseStatus/${uid}`).set({ active: false, expiry: 0 });
    return { valid: false, reason: "UNBOUND" };
  }
  const { key, rec } = found as { key: string; rec: LicenseRecord };

  if (rec.isActive !== true) {
    await writeStatusMirror(uid, rec);
    return { valid: false, reason: "REVOKED" };
  }
  if (rec.expiryDate <= Date.now()) {
    await db().ref(`licenses/${key}/isActive`).set(false);
    await writeStatusMirror(uid, { ...rec, isActive: false });
    return { valid: false, reason: "EXPIRED" };
  }

  await db().ref(`licenses/${key}/lastSeenAt`).set(Date.now());
  await writeStatusMirror(uid, rec);
  return { valid: true, expiryDate: rec.expiryDate };
});

// ---------------------------------------------------------------------------------------------
// Admin app
// ---------------------------------------------------------------------------------------------

/**
 * One-time bootstrap: grants the admin claim to ADMIN_EMAIL. Safe to leave deployed because it
 * only ever acts on that one address, and the caller must already be signed in as it.
 */
export const bootstrapAdmin = onCall(async (req) => {
  const email = req.auth?.token?.email;
  if (!email || email.toLowerCase() !== ADMIN_EMAIL.toLowerCase() || req.auth?.token?.email_verified !== true) {
    throw new HttpsError("permission-denied", "Administrator access is required.");
  }
  await getAuth().setCustomUserClaims(req.auth.uid, { admin: true });
  return { ok: true, note: "Sign out and back in for the claim to take effect." };
});

export const adminCreateLicense = onCall({ enforceAppCheck: true }, async (req) => {
  requireAdmin(req);
  const expiryDate = Number(req.data?.expiryDate);
  if (!Number.isFinite(expiryDate) || expiryDate <= Date.now()) {
    throw new HttpsError("invalid-argument", "Expiry must be a future date.");
  }
  const note = typeof req.data?.note === "string" ? req.data.note.slice(0, 120) : "";

  for (let attempt = 0; attempt < 5; attempt++) {
    const licenseKey = generateKey();
    const ref = db().ref(`licenses/${licenseKey}`);
    const rec: LicenseRecord = {
      isActive: true,
      boundDeviceId: null,
      expiryDate,
      createdAt: Date.now(),
      note,
    };
    const txn = await ref.transaction((current) => (current === null ? rec : undefined));
    if (txn.committed) {
      await writeStatusMirror(uidFor(licenseKey), rec);
      return { licenseKey, ...rec };
    }
  }
  throw new HttpsError("internal", "Could not allocate a unique key.");
});

export const adminListLicenses = onCall({ enforceAppCheck: true }, async (req) => {
  requireAdmin(req);
  const snap = await db().ref("licenses").orderByChild("createdAt").limitToLast(500).get();
  const out: Array<LicenseRecord & { licenseKey: string }> = [];
  snap.forEach((c) => {
    out.push({ licenseKey: c.key as string, ...(c.val() as LicenseRecord) });
    return false;
  });
  return { licenses: out.reverse() };
});

/** Frees a key for a new phone. Revokes refresh tokens so the old handset loses its session now. */
export const adminUnbindDevice = onCall({ enforceAppCheck: true }, async (req) => {
  requireAdmin(req);
  const licenseKey = normaliseKey(req.data?.licenseKey);
  const ref = db().ref(`licenses/${licenseKey}`);
  if (!(await ref.get()).exists()) throw new HttpsError("not-found", "No such licence key.");

  await ref.update({ boundDeviceId: null, activatedAt: null });
  const uid = uidFor(licenseKey);
  await getAuth().revokeRefreshTokens(uid).catch(() => undefined); // no session yet is fine
  await db().ref(`licenseStatus/${uid}`).set({ active: false, expiry: 0 });
  return { ok: true, licenseKey };
});

export const adminSetActive = onCall({ enforceAppCheck: true }, async (req) => {
  requireAdmin(req);
  const licenseKey = normaliseKey(req.data?.licenseKey);
  const isActive = req.data?.isActive === true;
  const ref = db().ref(`licenses/${licenseKey}`);
  const snap = await ref.get();
  if (!snap.exists()) throw new HttpsError("not-found", "No such licence key.");

  await ref.update({ isActive });
  const uid = uidFor(licenseKey);
  await writeStatusMirror(uid, { ...(snap.val() as LicenseRecord), isActive });
  if (!isActive) await getAuth().revokeRefreshTokens(uid).catch(() => undefined);
  return { ok: true, licenseKey, isActive };
});

// ---------------------------------------------------------------------------------------------
// Scheduled
// ---------------------------------------------------------------------------------------------

/** Deactivates licences whose expiry has passed. The heartbeat also catches this between runs. */
export const dailyExpiryCheck = onSchedule(
  { schedule: "17 2 * * *", timeZone: "Asia/Kolkata" },
  async () => {
    const now = Date.now();
    const snap = await db().ref("licenses").orderByChild("expiryDate").endAt(now).get();
    const updates: Record<string, unknown> = {};
    const expiredUids: string[] = [];

    snap.forEach((c) => {
      const rec = c.val() as LicenseRecord;
      if (rec.isActive === true) {
        updates[`licenses/${c.key}/isActive`] = false;
        const uid = uidFor(c.key as string);
        updates[`licenseStatus/${uid}/active`] = false;
        expiredUids.push(uid);
      }
      return false;
    });

    if (Object.keys(updates).length > 0) await db().ref().update(updates);
    await Promise.all(expiredUids.map((uid) => getAuth().revokeRefreshTokens(uid).catch(() => undefined)));
    console.log(`dailyExpiryCheck: deactivated ${expiredUids.length} licence(s).`);
  }
);
