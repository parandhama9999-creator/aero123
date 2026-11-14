package com.example.license

import android.content.Context
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** What the UI should show. Never the thing that guards the data — the database rules do that. */
sealed interface LicenseState {
    data object Checking : LicenseState
    data object NeedsActivation : LicenseState
    data class Active(val expiryDate: Long) : LicenseState
    data class Blocked(val reason: String, val message: String) : LicenseState
}

/**
 * Client half of the licence system.
 *
 * Deliberately holds no authority. Patching this class to report [LicenseState.Active] gains an
 * attacker nothing: without a custom token minted by the server there is no `auth`, and
 * `/aerofast_devices` is unreadable and unwritable. The gate is in the rules; this is the UI's
 * view of it.
 */
object LicenseManager {

    private const val TAG = "LicenseManager"
    private const val REGION = "asia-southeast1"
    private const val PREFS = "aerofast_license"
    private const val KEY_LAST_OK = "last_verified_at"
    private const val KEY_EXPIRY = "cached_expiry"

    /** How long the app keeps working without reaching the server, for genuinely offline users. */
    private const val OFFLINE_GRACE_MS = 7L * 24 * 60 * 60 * 1000

    /** How often to re-check while online. */
    private const val HEARTBEAT_INTERVAL_MS = 24L * 60 * 60 * 1000

    private fun prefs(context: Context) = EncryptedSharedPreferences.create(
        context,
        PREFS,
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private fun functions() = FirebaseFunctions.getInstance(REGION)

    /** Exchanges a licence key for a Firebase session. Throws [LicenseError] with a shown message. */
    suspend fun activate(context: Context, licenseKey: String): LicenseState {
        val payload = mapOf(
            "licenseKey" to licenseKey.trim().uppercase(),
            "deviceId" to DeviceIdentity.get(context)
        )
        val result = try {
            functions().getHttpsCallable("activateLicense").call(payload).awaitResult()
        } catch (e: FirebaseFunctionsException) {
            throw LicenseError(e.message ?: "Activation failed. Please try again.")
        } catch (e: Exception) {
            throw LicenseError("Could not reach the activation server. Check your connection.")
        }

        @Suppress("UNCHECKED_CAST")
        val data = result.data as? Map<String, Any?> ?: throw LicenseError("Unexpected server response.")
        val token = data["token"] as? String ?: throw LicenseError("Unexpected server response.")
        val expiry = (data["expiryDate"] as? Number)?.toLong() ?: 0L

        FirebaseAuth.getInstance().signInWithCustomToken(token).awaitResult()
        recordSuccess(context, expiry)
        return LicenseState.Active(expiry)
    }

    /**
     * Called on app open. Re-reads live server state rather than the token, so a revoke or an
     * unbind lands on the next open rather than waiting for the token to expire.
     */
    suspend fun verify(context: Context): LicenseState {
        if (FirebaseAuth.getInstance().currentUser == null) return LicenseState.NeedsActivation

        val store = prefs(context)
        val lastOk = store.getLong(KEY_LAST_OK, 0L)
        val cachedExpiry = store.getLong(KEY_EXPIRY, 0L)
        val now = System.currentTimeMillis()

        if (cachedExpiry in 1 until now) {
            return LicenseState.Blocked("EXPIRED", "Your licence has expired. Contact the administrator to renew it.")
        }
        if (now - lastOk < HEARTBEAT_INTERVAL_MS) return LicenseState.Active(cachedExpiry)

        val result = try {
            functions().getHttpsCallable("heartbeat")
                .call(mapOf("deviceId" to DeviceIdentity.get(context)))
                .awaitResult()
        } catch (e: Exception) {
            Log.w(TAG, "Heartbeat unreachable: ${e.message}")
            // Offline: keep working inside the grace window, then stop.
            return if (lastOk > 0 && now - lastOk < OFFLINE_GRACE_MS) LicenseState.Active(cachedExpiry)
            else LicenseState.Blocked("OFFLINE", "Connect to the internet to verify your licence.")
        }

        @Suppress("UNCHECKED_CAST")
        val data = result.data as? Map<String, Any?> ?: return LicenseState.Active(cachedExpiry)
        if (data["valid"] == true) {
            val expiry = (data["expiryDate"] as? Number)?.toLong() ?: cachedExpiry
            recordSuccess(context, expiry)
            return LicenseState.Active(expiry)
        }

        signOut(context)
        return when (data["reason"]) {
            "EXPIRED" -> LicenseState.Blocked("EXPIRED", "Your licence has expired. Contact the administrator to renew it.")
            "REVOKED" -> LicenseState.Blocked("REVOKED", "This licence has been revoked. Contact the administrator.")
            "UNBOUND" -> LicenseState.Blocked("UNBOUND", "This device is no longer registered. Enter your key to activate it again.")
            else -> LicenseState.NeedsActivation
        }
    }

    fun signOut(context: Context) {
        FirebaseAuth.getInstance().signOut()
        prefs(context).edit().clear().apply()
    }

    private fun recordSuccess(context: Context, expiry: Long) {
        prefs(context).edit()
            .putLong(KEY_LAST_OK, System.currentTimeMillis())
            .putLong(KEY_EXPIRY, expiry)
            .apply()
    }
}

class LicenseError(message: String) : Exception(message)

/** Bridges a Play Services [Task] to a coroutine without pulling in coroutines-play-services. */
private suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { cont.resume(it) }
    addOnFailureListener { cont.resumeWithException(it) }
    addOnCanceledListener { cont.cancel() }
}
