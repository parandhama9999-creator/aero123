package com.aistudio.aerofast.trmst.ui.platform

import androidx.compose.runtime.staticCompositionLocalOf

/** What a shared screen needs from the device: share or export text, and keep small settings. */
interface PlatformActions {
    fun shareText(subject: String, text: String)
    fun loadString(store: String, key: String): String?
    fun saveString(store: String, key: String, value: String)
}

val LocalPlatformActions = staticCompositionLocalOf<PlatformActions> { error("PlatformActions not provided") }
