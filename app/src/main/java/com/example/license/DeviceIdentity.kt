package com.example.license

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import java.security.MessageDigest

/**
 * The device half of "one key, one device".
 *
 * ANDROID_ID is the most stable identifier Android still offers an ordinary app: it survives an
 * uninstall and reinstall, and changes only on a factory reset. That matters commercially — a
 * Firebase Installation ID would rotate every time a customer cleared app data, and each of those
 * would land on the admin as an unbind request.
 *
 * It is not unspoofable; a rooted phone can report whatever it likes. That gap is closed by App
 * Check rather than by this file, which is the right place for it: Play Integrity's verdict is
 * produced by Google and checked on the server, where the user cannot reach it.
 */
object DeviceIdentity {

    @SuppressLint("HardwareIds")
    fun get(context: Context): String {
        val raw = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        // A handful of devices return null or the known-broken 9774d56d682e549c
        val seed = if (raw.isNullOrBlank() || raw == "9774d56d682e549c") fallbackId(context) else raw
        return sha256(seed).take(40)
    }

    /** A per-install id for the rare handset with no usable ANDROID_ID. Resets on reinstall. */
    private fun fallbackId(context: Context): String {
        val prefs = context.getSharedPreferences("aerofast_device", Context.MODE_PRIVATE)
        prefs.getString("fallback_id", null)?.let { return it }
        val generated = java.util.UUID.randomUUID().toString()
        prefs.edit().putString("fallback_id", generated).apply()
        return generated
    }

    private fun sha256(input: String): String =
        MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
            .joinToString("") { "%02x".format(it) }
}
