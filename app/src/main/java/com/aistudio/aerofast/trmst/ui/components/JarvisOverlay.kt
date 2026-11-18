package com.aistudio.aerofast.trmst.ui.components

import android.content.Context
import android.speech.tts.TextToSpeech
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.aerofast.trmst.data.JarvisReply
import com.aistudio.aerofast.trmst.ui.theme.LocalCustomColors
import com.aistudio.aerofast.trmst.util.AppLanguage
import java.util.Locale

/** Jarvis's voice: Android text-to-speech in the chosen app language. */
class JarvisVoice(context: Context) {
    private var ready = false
    private val tts: TextToSpeech = TextToSpeech(context.applicationContext) { status -> ready = status == TextToSpeech.SUCCESS }

    /** Speaks [text]; false when the engine is not ready or has no voice for [lang] (the reply is still shown). */
    fun speak(text: String, lang: AppLanguage): Boolean {
        if (!ready || text.isBlank()) return false
        val locale = Locale.forLanguageTag(JarvisReply.localeTag(lang))
        val set = tts.setLanguage(locale)
        if (set == TextToSpeech.LANG_MISSING_DATA || set == TextToSpeech.LANG_NOT_SUPPORTED) return false
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "jarvis")
        return true
    }

    fun stop() { if (ready) tts.stop() }
    fun shutdown() { tts.stop(); tts.shutdown() }
}

@Composable
fun JarvisFab(onClick: () -> Unit) {
    val c = LocalCustomColors.current
    FloatingActionButton(onClick = onClick, containerColor = c.accentPrimary, shape = CircleShape) {
        Icon(Icons.Default.Mic, contentDescription = "Jarvis", tint = c.bgCard)
    }
}

/** What Jarvis heard and answered, the language it listens and speaks in, and the mic. */
@Composable
fun JarvisPanel(
    language: AppLanguage,
    heard: String,
    reply: String,
    busy: Boolean,
    onLanguage: (AppLanguage) -> Unit,
    onMic: () -> Unit,
    onStop: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val c = LocalCustomColors.current
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = c.bgCard,
        border = BorderStroke(1.5.dp, c.accentPrimary),
        shadowElevation = 8.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("JARVIS", fontSize = 13.sp, fontWeight = FontWeight.Black, color = c.accentPrimary)
                Spacer(Modifier.size(8.dp))
                Text(if (busy) "…" else JarvisReply.localeTag(language), fontSize = 11.sp, color = c.textMuted, modifier = Modifier.weight(1f))
                IconButton(onClick = onStop, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Stop, contentDescription = "Stop speaking", tint = c.textMuted) }
                IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Close, contentDescription = "Close Jarvis", tint = c.textMuted) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                AppLanguage.values().forEach { l ->
                    val selected = l == language
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (selected) c.accentPrimary else c.bgElevated,
                        border = BorderStroke(1.dp, if (selected) c.accentPrimary else c.border),
                        modifier = Modifier.clickable { onLanguage(l) }
                    ) {
                        Text(l.displayName, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (selected) c.bgCard else c.textMain,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                    }
                }
            }
            Column(modifier = Modifier.heightIn(max = 200.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (heard.isNotBlank()) Text("🗣 $heard", fontSize = 12.sp, color = c.textMuted)
                if (reply.isNotBlank()) Text(reply, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = c.textMain)
                if (heard.isBlank() && reply.isBlank()) Text(JarvisReply.notUnderstood(language).substringAfter(". ").substringAfter("。"), fontSize = 11.sp, color = c.textMuted)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                if (busy) CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 3.dp, color = c.accentPrimary)
                else FloatingActionButton(onClick = onMic, containerColor = c.accentPrimary, shape = CircleShape, modifier = Modifier.size(52.dp)) {
                    Icon(Icons.Default.Mic, contentDescription = "Speak to Jarvis", tint = c.bgCard)
                }
            }
        }
    }
}
