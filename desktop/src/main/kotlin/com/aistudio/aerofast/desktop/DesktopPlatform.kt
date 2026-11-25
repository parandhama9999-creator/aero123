package com.aistudio.aerofast.desktop

import com.aistudio.aerofast.trmst.ui.platform.PlatformActions
import java.awt.FileDialog
import java.awt.Frame
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.File
import java.util.prefs.Preferences

/** Desktop side of the shared screens: "share" copies the text to the clipboard and offers to save it as a .txt file. */
class DesktopPlatform(private val frame: () -> Frame?) : PlatformActions {
    var lastMessage: (String) -> Unit = {}

    override fun shareText(subject: String, text: String) {
        Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null)
        val dialog = FileDialog(frame(), "Save $subject", FileDialog.SAVE).apply {
            file = subject.replace(Regex("[^A-Za-z0-9 ._-]+"), "_").trim() + ".txt"
            isVisible = true
        }
        val name = dialog.file
        if (name == null) {
            lastMessage("Copied to the clipboard")
            return
        }
        val target = File(dialog.directory, if (name.contains('.')) name else "$name.txt")
        target.writeText(text)
        lastMessage("Saved ${target.absolutePath} (also copied to the clipboard)")
    }

    private fun node(store: String) = Preferences.userRoot().node("aerofast-pro/$store")

    override fun loadString(store: String, key: String): String? = node(store).get(key, null)

    override fun saveString(store: String, key: String, value: String) {
        node(store).put(key, value)
    }
}
