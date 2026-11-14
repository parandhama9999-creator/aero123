package com.example.license

import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory

/**
 * Debug builds only. Prints a token to logcat that must be registered under
 * App Check > Apps > Manage debug tokens before a debug build can call anything.
 */
fun installAppCheck() {
    FirebaseAppCheck.getInstance()
        .installAppCheckProviderFactory(DebugAppCheckProviderFactory.getInstance())
}
