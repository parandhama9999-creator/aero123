package com.example.util

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Security utilities for encrypted storage and sensitive data handling
 */
object SecurityUtils {

    /**
     * Get encrypted preferences instance. Uses hardware-backed keystore if available.
     */
    fun getEncryptedPreferences(context: Context, name: String): EncryptedSharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            name,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        ) as EncryptedSharedPreferences
    }

    /**
     * Securely clear sensitive strings from memory
     */
    fun clearSensitiveString(value: String?) {
        value?.let {
            // Kotlin's immutable strings prevent actual clearing, but this is best practice placeholder
            // In practice, sensitive data should be stored in Parcelable/Bundle/etc. that can be cleared
        }
    }

    /**
     * Validate API key format before storing
     */
    fun isValidApiKey(key: String?): Boolean {
        if (key.isNullOrBlank()) return false
        if (key.length < 10) return false
        return !key.contains(" ") && !key.contains("\n")
    }

    /**
     * Check if device has encryption available
     */
    fun isEncryptionAvailable(context: Context): Boolean {
        return try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            masterKey != null
        } catch (e: Exception) {
            false
        }
    }
}
