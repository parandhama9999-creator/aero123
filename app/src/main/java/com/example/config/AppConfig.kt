package com.example.config

/**
 * Centralized configuration for AeroFast Pro.
 * Keep sensitive values out of source code.
 */
object AppConfig {
    // Firebase Configuration
    const val FIREBASE_DB_URL = "https://aerofast-pro-default-rtdb.asia-southeast1.firebasedatabase.app"

    // API Configuration
    const val GEMINI_API_ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/models"

    // Timeouts (in seconds)
    const val API_CONNECT_TIMEOUT = 30L
    const val API_READ_TIMEOUT = 60L
    const val API_WRITE_TIMEOUT = 60L
    const val API_CALL_TIMEOUT = 90L

    // Rate Limiting
    const val API_CALL_MIN_INTERVAL_MS = 500L // Min 500ms between API calls

    // Caching
    const val CACHE_VALIDITY_DAYS = 30L
    const val CACHE_VALIDITY_MS = CACHE_VALIDITY_DAYS * 24L * 60L * 60L * 1000L

    // Data Limits
    const val MAX_IMAGE_DIMENSION = 1600
    const val IMAGE_QUALITY_PERCENT = 85

    // Security
    /**
     * The encrypted store MUST NOT share a filename with any plaintext SharedPreferences file.
     * "aerofast_prefs" is opened in plaintext by the disclaimer flag, the settings dialog and
     * AppLocalization; two SharedPreferences instances over one XML file overwrite each other's
     * entries, which silently loses the saved API key. [PREFERENCES_LEGACY_NAME] is read once so a
     * key saved by an older build is migrated, then left alone.
     */
    const val PREFERENCES_ENCRYPTED_NAME = "aerofast_prefs_secure"
    const val PREFERENCES_LEGACY_NAME = "aerofast_prefs"
    const val SCAN_CACHE_ENCRYPTED_NAME = "aerofast_scan_cache"
    const val KEY_GEMINI_PRIMARY = "aerofast_gemini_key"
    const val KEY_GEMINI_LEGACY = "user_gemini_key"
}
