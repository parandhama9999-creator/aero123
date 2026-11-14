package com.example.admin

import com.google.android.gms.tasks.Task
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Admin operations. This file is compiled only into the `admin` flavour, so none of it — not the
 * function names, not the strings — exists inside the Play Store APK to be found and probed.
 *
 * Authority rests on the `admin` custom claim checked inside each Cloud Function. Reaching these
 * endpoints from a decompiled copy of this app achieves nothing without that claim.
 */
object AdminPanel {

    private const val REGION = "asia-southeast1"
    private fun fn(name: String) = FirebaseFunctions.getInstance(REGION).getHttpsCallable(name)

    data class License(
        val licenseKey: String,
        val isActive: Boolean,
        val boundDeviceId: String?,
        val expiryDate: Long,
        val createdAt: Long,
        val note: String,
        val lastSeenAt: Long?
    )

    /** Mints a key. [expiryDate] is epoch millis. */
    suspend fun createLicense(expiryDate: Long, note: String = ""): License {
        val data = fn("adminCreateLicense")
            .call(mapOf("expiryDate" to expiryDate, "note" to note))
            .awaitResult().data.asMap()
        return data.toLicense()
    }

    suspend fun listLicenses(): List<License> {
        val data = fn("adminListLicenses").call().awaitResult().data.asMap()
        val rows = data["licenses"] as? List<*> ?: emptyList<Any?>()
        return rows.mapNotNull { (it as? Map<*, *>)?.toLicense() }
    }

    /** Frees a key so the customer can activate it on a new phone. */
    suspend fun unbindDevice(licenseKey: String) {
        fn("adminUnbindDevice").call(mapOf("licenseKey" to licenseKey)).awaitResult()
    }

    suspend fun setActive(licenseKey: String, isActive: Boolean) {
        fn("adminSetActive")
            .call(mapOf("licenseKey" to licenseKey, "isActive" to isActive))
            .awaitResult()
    }

    /** Run once, signed in as the admin address, to grant this account its claim. */
    suspend fun bootstrapAdmin() {
        fn("bootstrapAdmin").call().awaitResult()
    }

    private fun Any?.asMap(): Map<*, *> = this as? Map<*, *> ?: emptyMap<Any?, Any?>()

    private fun Map<*, *>.toLicense() = License(
        licenseKey = this["licenseKey"] as? String ?: "",
        isActive = this["isActive"] as? Boolean ?: false,
        boundDeviceId = this["boundDeviceId"] as? String,
        expiryDate = (this["expiryDate"] as? Number)?.toLong() ?: 0L,
        createdAt = (this["createdAt"] as? Number)?.toLong() ?: 0L,
        note = this["note"] as? String ?: "",
        lastSeenAt = (this["lastSeenAt"] as? Number)?.toLong()
    )
}

private suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { cont.resume(it) }
    addOnFailureListener { cont.resumeWithException(it) }
    addOnCanceledListener { cont.cancel() }
}
