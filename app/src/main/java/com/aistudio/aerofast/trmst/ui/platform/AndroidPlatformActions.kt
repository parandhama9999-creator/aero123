package com.aistudio.aerofast.trmst.ui.platform

import android.content.Context
import android.content.Intent

class AndroidPlatformActions(private val context: Context) : PlatformActions {
    override fun shareText(subject: String, text: String) {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(send, subject))
    }

    override fun loadString(store: String, key: String): String? =
        context.getSharedPreferences(store, Context.MODE_PRIVATE).getString(key, null)

    override fun saveString(store: String, key: String, value: String) {
        context.getSharedPreferences(store, Context.MODE_PRIVATE).edit().putString(key, value).apply()
    }
}
