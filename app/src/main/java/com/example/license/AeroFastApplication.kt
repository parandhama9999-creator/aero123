package com.example.license

import android.app.Application
import com.google.firebase.FirebaseApp

/**
 * Installs App Check before anything can touch Firebase.
 *
 * App Check is what makes the licence system hold up against a repackaged APK: Play Integrity has
 * Google attest, server-side, that the call came from a build signed with our key, installed by
 * Play, on a device that passes integrity. A cracked build cannot produce that token, so every
 * Cloud Function and database request it makes is refused no matter what the patched client thinks.
 *
 * The provider itself comes from the build type's source set — Play Integrity for release, the
 * debug provider for debug — so the two can never be confused at runtime.
 */
class AeroFastApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        installAppCheck()
    }
}
