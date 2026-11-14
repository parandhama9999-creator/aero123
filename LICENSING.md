# AeroFast Pro — licensing architecture

One codebase, two apps, one Firebase project.

## Where authority lives

Nothing in the APK decides anything. A patched client that forces its own licence screen to say
"Active" gains no data, because `/aerofast_devices` is unreadable without a custom token that only
`activateLicense` can mint. Treat every client-side check here as user interface, not security.

| Enforced by | What it stops |
|---|---|
| Cloud Functions (`functions/src/index.ts`) | Binding a key to a second device; minting, revoking or unbinding without the admin claim |
| Database rules (`database.rules.json`) | Reading or writing another licence's data; reading `/licenses` at all; writing malformed state |
| App Check / Play Integrity | A repackaged or emulated build calling any of it |
| Product flavours | The admin code path existing inside the Play Store APK |

## Layout

```
functions/src/index.ts              activateLicense, heartbeat, dailyExpiryCheck, admin*
database.rules.json                 the rules below
firebase.json                       deploy config

app/src/main/java/com/example/license/
    AeroFastApplication.kt          installs App Check at startup
    AppCheckProvider.kt             (src/debug + src/release) per-build-type provider
    DeviceIdentity.kt               ANDROID_ID, hashed
    LicenseManager.kt               activate / verify / signOut
    ActivationScreen.kt             key entry

app/src/user/java/com/example/gate/AppGate.kt     licence gate   (Play build)
app/src/admin/java/com/example/gate/AppGate.kt    admin sign-in  (private build)
app/src/admin/java/com/example/admin/AdminPanel.kt  create / list / unbind / revoke
```

`./gradlew assembleUserRelease` → Play Store. `assembleAdminRelease` → your own phone, sideloaded.
The admin sources are not compiled into the user flavour, so they cannot be found by decompiling it.

## Data model

```
/licenses/{AERO-XXXX-YYYY}   isActive, boundDeviceId, expiryDate, createdAt, note, lastSeenAt
                             admin-only; the user app never reads this
/licenseStatus/{uid}         active, expiry  — written only by Functions, read only by rules
/aerofast_devices/{uid}      app state, keyed by the signed-in uid
```

`uid = "lic_" + sha256(licenseKey)[0:32]`. The plaintext key never appears in a token or a rule, so
the rules can gate on licence state without `/licenses` being readable by anyone but you.

`/licenseStatus` is the mirror the rules read. Rules evaluate with full server privileges regardless
of `.read`, which is why it can be `false` for everyone and still work. Revocation therefore lands on
the next request rather than waiting for a token to expire.

## Setup, in order

1. **Upgrade to Blaze.** Cloud Functions are unavailable on Spark. Expect well under $1/month at
   your volume, but a card is required.
2. **Enable Email/Password auth** (Authentication → Sign-in method) and create your admin account.
3. **Re-download `google-services.json`** from Project settings into `app/`, after the Realtime
   Database exists. The SDK reads the database URL from it, and without the `asia-southeast1` entry
   `FirebaseDatabase.getInstance()` points at the wrong region. The file is not in this repo.
4. **Deploy:** `firebase deploy --only functions,database`
5. **Claim admin:** sign into the admin build once, call `AdminPanel.bootstrapAdmin()`, then sign out
   and back in. `bootstrapAdmin` only ever acts on `ADMIN_EMAIL` in `index.ts` — change that constant
   before deploying.
6. **App Check:** Firebase Console → App Check → register the Android app with **Play Integrity**,
   then set Realtime Database and Cloud Functions to **Enforced**. Do this only after a release build
   is on Play, or you will lock yourself out of your own app.
7. **Debug builds:** run one, copy the debug token from logcat, and add it under
   App Check → Apps → Manage debug tokens. Without this, debug builds cannot call anything.

## Root and emulator policy

Play Integrity returns device verdicts, and App Check turns them into a yes or no before your code
runs. `MEETS_DEVICE_INTEGRITY` is the signal to require; it already excludes emulators and most
rooted handsets, which is why there is no root-detection code in the app — a check that runs on the
attacker's device is the one thing they can always remove.

Decide this deliberately rather than by default: requiring it also turns away customers on legitimate
custom ROMs, and for a paid engineering tool those are real users. The looser
`MEETS_BASIC_INTEGRITY` keeps them and still blocks casual emulator use.

## Honest limits

- **Device identity can be spoofed on a rooted phone.** ANDROID_ID is self-reported. App Check is the
  answer, not the device id, which is why root bypass and licence bypass are the same problem here.
- **Two phones, one person, one key** is enforced; one phone shared by two people is not, and cannot
  be by any client-side means.
- **Obfuscation buys time, not safety.** The R8 rules make the flow tedious to read. They are not
  load-bearing, and nothing in the design assumes they hold.
- **The offline grace window is a business decision.** `LicenseManager.OFFLINE_GRACE_MS` is 7 days:
  long enough for a shop floor with poor signal, short enough that airplane mode is not a bypass.
