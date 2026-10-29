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
    const val PREFERENCES_ENCRYPTED_NAME = "aerofast_prefs"
    const val SCAN_CACHE_ENCRYPTED_NAME = "aerofast_scan_cache"
}
