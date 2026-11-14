package com.example.license

import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory

/**
 * Release builds only. The debug provider is not merely unused here — it is not on the release
 * classpath at all, so no shipped APK can be coaxed into accepting a debug token.
 */
fun installAppCheck() {
    FirebaseAppCheck.getInstance()
        .installAppCheckProviderFactory(PlayIntegrityAppCheckProviderFactory.getInstance())
}
