package com.aistudio.aerofast.trmst.ui.screens

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.aerofast.trmst.data.AiSettings
import com.aistudio.aerofast.trmst.data.FastenerCalculationResult
import com.aistudio.aerofast.trmst.data.GeminiAiService
import com.aistudio.aerofast.trmst.data.MachineSetupStore
import com.aistudio.aerofast.trmst.data.MediaAi
import com.aistudio.aerofast.trmst.data.MediaModel
import com.aistudio.aerofast.trmst.data.VideoJob
import com.aistudio.aerofast.trmst.ui.theme.AlertRed
import com.aistudio.aerofast.trmst.ui.theme.LocalCustomColors
import com.aistudio.aerofast.trmst.ui.theme.SuccessGreen
import com.aistudio.aerofast.trmst.util.DrawingExporter
import com.aistudio.aerofast.trmst.util.HeadingVideoEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** Exact drawings (PNG / PDF), the heading animation (MP4), and optional AI illustrations through the Gemini key. */
@Composable
fun MediaStudioScreen(result: FastenerCalculationResult) {
    val c = LocalCustomColors.current
    Column(
        Modifier.fillMaxSize().background(c.bgMain).verticalScroll(rememberScrollState()).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("IMAGES & VIDEO", fontSize = 14.sp, fontWeight = FontWeight.Black, color = c.accentPrimary)
        Text("${result.threadLabel} × ${"%.1f".format(java.util.Locale.US, result.lengthMm)} mm ${result.fastenerType.label}, ${result.material.key}",
            fontSize = 11.sp, fontWeight = FontWeight.Bold, color = c.textMain)
        ExactDrawingsCard(result)
        AnimationCard(result)
        AiMediaCard(result)
    }
}

@Composable
private fun Card(title: String, accent: Color, content: @Composable () -> Unit) {
    val c = LocalCustomColors.current
    Surface(shape = RoundedCornerShape(10.dp), color = c.bgCard, border = androidx.compose.foundation.BorderStroke(1.dp, c.border), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, fontSize = 11.sp, fontWeight = FontWeight.Black, color = accent)
            content()
        }
    }
}

@Composable
private fun ActionButton(label: String, enabled: Boolean = true, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val c = LocalCustomColors.current
    Button(onClick = onClick, enabled = enabled, colors = ButtonDefaults.buttonColors(containerColor = c.accentPrimary), modifier = modifier) {
        Text(label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}

@Composable
private fun ExactDrawingsCard(r: FastenerCalculationResult) {
    val c = LocalCustomColors.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val preview = remember(r) { DrawingExporter.progressionImage(r, 1200, 750)?.asImageBitmap() }
    Card("1. EXACT DRAWINGS — from the app's calculation (offline)", SuccessGreen) {
        Text("Progression, finished part and every station's tool section, drawn from the calculated dimensions. Share as images or as one A4 PDF with the tooling sheet and production report.",
            fontSize = 10.sp, color = c.textMuted)
        preview?.let { Image(it, "Heading progression", Modifier.fillMaxWidth(), contentScale = ContentScale.FillWidth) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            ActionButton("Share images (PNG)", !busy, Modifier.weight(1f)) {
                busy = true; error = null
                scope.launch {
                    runCatching { withContext(Dispatchers.Default) { DrawingExporter.pngFiles(context, r, MachineSetupStore.setup) } }
                        .onSuccess { DrawingExporter.share(context, it, "image/png", "AeroFast drawings ${r.threadLabel}") }
                        .onFailure { error = it.message }
                    busy = false
                }
            }
            ActionButton("Share PDF", !busy, Modifier.weight(1f)) {
                busy = true; error = null
                scope.launch {
                    runCatching { withContext(Dispatchers.Default) { DrawingExporter.pdfFile(context, r, MachineSetupStore.setup) } }
                        .onSuccess { DrawingExporter.share(context, listOf(it), "application/pdf", "AeroFast drawing pack ${r.threadLabel}") }
                        .onFailure { error = it.message }
                    busy = false
                }
            }
        }
        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        error?.let { Text("⛔ $it", fontSize = 10.sp, color = AlertRed) }
    }
}

@Composable
private fun AnimationCard(r: FastenerCalculationResult) {
    val c = LocalCustomColors.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val plan = r.headingPlan
    var playing by remember { mutableStateOf(true) }
    var progress by remember { mutableStateOf<Float?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val label = "${r.threadLabel} × ${"%.1f".format(java.util.Locale.US, r.lengthMm)} mm ${r.fastenerType.label.substringBefore(" (")} • ${r.material.key}"
    Card("2. HEADING ANIMATION — every blow, offline MP4", SuccessGreen) {
        if (plan == null || plan.stations.size < 2) {
            Text("No heading sequence for this part.", fontSize = 11.sp, color = c.textMuted)
            return@Card
        }
        Text("Each key shape is a station from the app; the metal between stations is interpolated by volume, not simulated.", fontSize = 10.sp, color = c.textMuted)
        HeadingAnimationPlayer(plan, label, playing)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            ActionButton(if (playing) "Pause" else "Play", modifier = Modifier.weight(1f)) { playing = !playing }
            ActionButton("Save MP4 & share", progress == null, Modifier.weight(1.4f)) {
                error = null; progress = 0f
                scope.launch {
                    runCatching {
                        withContext(Dispatchers.Default) {
                            val dir = File(context.cacheDir, "drawings").apply { mkdirs() }
                            val out = File(dir, "AeroFast_${r.threadLabel.replace(Regex("[^A-Za-z0-9]+"), "_")}_heading.mp4")
                            HeadingVideoEncoder.encode(plan, label, out) { p -> progress = p }
                        }
                    }.onSuccess { DrawingExporter.share(context, listOf(it), "video/mp4", "AeroFast heading animation ${r.threadLabel}") }
                        .onFailure { error = "Could not write the MP4 on this phone: ${it.message}" }
                    progress = null
                }
            }
        }
        progress?.let { LinearProgressIndicator(progress = { it }, modifier = Modifier.fillMaxWidth()); Text("Encoding ${(it * 100).toInt()} %", fontSize = 10.sp, color = c.textMuted) }
        error?.let { Text("⛔ $it", fontSize = 10.sp, color = AlertRed) }
    }
}

private const val MEDIA_PREFS = "aerofast_media_ai"

@Composable
private fun AiMediaCard(r: FastenerCalculationResult) {
    val c = LocalCustomColors.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences(MEDIA_PREFS, Context.MODE_PRIVATE) }
    val key = remember { GeminiAiService.getEffectiveApiKey(context) }
    val offline = remember { AiSettings.offlineOnly(context) }
    var imageModels by remember { mutableStateOf<List<MediaModel>>(emptyList()) }
    var videoModels by remember { mutableStateOf<List<MediaModel>>(emptyList()) }
    var imageModel by remember { mutableStateOf(prefs.getString("image_model", "").orEmpty()) }
    var videoModel by remember { mutableStateOf(prefs.getString("video_model", "").orEmpty()) }
    var imagePrompt by remember(r) { mutableStateOf(MediaAi.imagePrompt(r)) }
    var videoPrompt by remember(r) { mutableStateOf(MediaAi.videoPrompt(r)) }
    var status by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var imageFile by remember { mutableStateOf<File?>(null) }
    var videoFile by remember { mutableStateOf<File?>(null) }

    Card("3. AI IMAGE & VIDEO — online, your Gemini key", c.accentSecondary) {
        Text("⚠ AI illustration — NOT to scale. The picture is invented by the AI model from the text; threads, hex and socket can be wrong. Use it for catalogues and presentations, never for engineering. Each image or video is billed to your Gemini key; video models may need a paid plan.",
            fontSize = 10.sp, color = AlertRed, fontWeight = FontWeight.Bold)
        when {
            offline -> { Text("Offline only is ON in Settings, so nothing is sent online. Turn it off to use AI images and video.", fontSize = 11.sp, color = c.textMuted); return@Card }
            key.isBlank() -> { Text("No Gemini key saved. Add one in Settings (Google AI Studio key) to use AI images and video.", fontSize = 11.sp, color = c.textMuted); return@Card }
        }
        ActionButton(if (imageModels.isEmpty() && videoModels.isEmpty()) "Load models from your key" else "Reload models", !busy, Modifier.fillMaxWidth()) {
            busy = true; status = null
            scope.launch {
                MediaAi.listModels(key).onSuccess { m ->
                    imageModels = m.image; videoModels = m.video
                    if (imageModel.isBlank() || m.image.none { it.id == imageModel }) imageModel = m.image.firstOrNull()?.id.orEmpty()
                    if (videoModel.isBlank() || m.video.none { it.id == videoModel }) videoModel = m.video.firstOrNull()?.id.orEmpty()
                    status = "Your key offers ${m.image.size} image model(s) and ${m.video.size} video model(s)."
                }.onFailure { status = "⛔ ${it.message}" }
                busy = false
            }
        }

        @Composable
        fun picker(models: List<MediaModel>, selected: String, onPick: (String) -> Unit) {
            models.forEach { m ->
                val sel = m.id == selected
                Surface(shape = RoundedCornerShape(8.dp), color = if (sel) c.accentPrimary else c.bgElevated,
                    modifier = Modifier.fillMaxWidth().clickable { onPick(m.id) }) {
                    Text("${m.displayName}  (${m.id})", fontSize = 10.sp, fontFamily = FontFamily.Monospace,
                        color = if (sel) Color.White else c.textMain, modifier = Modifier.padding(8.dp))
                }
            }
        }

        Text("AI IMAGE", fontSize = 11.sp, fontWeight = FontWeight.Black, color = c.textMain)
        picker(imageModels, imageModel) { imageModel = it; prefs.edit().putString("image_model", it).apply() }
        OutlinedTextField(imagePrompt, { imagePrompt = it }, label = { Text("Image prompt", fontSize = 10.sp) },
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, color = c.textMain), modifier = Modifier.fillMaxWidth())
        ActionButton("Generate image", !busy && imageModel.isNotBlank(), Modifier.fillMaxWidth()) {
            busy = true; status = "Generating image with $imageModel …"; imageFile = null
            scope.launch {
                MediaAi.generateImage(key, imageModel, imagePrompt).onSuccess { img ->
                    imageFile = withContext(Dispatchers.Default) { saveLabelled(context, img.bytes) }
                    status = if (imageFile == null) "⛔ The reply could not be read as an image." else "Image ready" + (img.note.takeIf { it.isNotBlank() }?.let { " — model note: $it" } ?: ".")
                }.onFailure { status = "⛔ ${it.message}" }
                busy = false
            }
        }
        imageFile?.let { f ->
            val bmp = remember(f) { BitmapFactory.decodeFile(f.absolutePath)?.asImageBitmap() }
            bmp?.let { Image(it, "AI illustration", Modifier.fillMaxWidth().heightIn(max = 420.dp), contentScale = ContentScale.Fit) }
            ActionButton("Share AI image", modifier = Modifier.fillMaxWidth()) { DrawingExporter.share(context, listOf(f), "image/png", "AI illustration (not to scale)") }
        }

        Text("AI VIDEO", fontSize = 11.sp, fontWeight = FontWeight.Black, color = c.textMain)
        if (videoModels.isEmpty() && imageModels.isNotEmpty()) Text("Your key lists no video (Veo) model.", fontSize = 10.sp, color = c.textMuted)
        picker(videoModels, videoModel) { videoModel = it; prefs.edit().putString("video_model", it).apply() }
        OutlinedTextField(videoPrompt, { videoPrompt = it }, label = { Text("Video prompt", fontSize = 10.sp) },
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, color = c.textMain), modifier = Modifier.fillMaxWidth())
        ActionButton("Generate video (takes minutes)", !busy && videoModel.isNotBlank(), Modifier.fillMaxWidth()) {
            busy = true; videoFile = null; status = "Starting video job with $videoModel …"
            scope.launch {
                val op = MediaAi.startVideo(key, videoModel, videoPrompt)
                op.onFailure { status = "⛔ ${it.message}"; busy = false }
                val name = op.getOrNull() ?: return@launch
                var waited = 0
                while (waited <= 15 * 60) {
                    delay(10_000); waited += 10
                    when (val job = MediaAi.pollVideo(key, name).getOrElse { VideoJob.Failed(it.message ?: "poll failed") }) {
                        is VideoJob.Running -> status = "Video is being generated … ${waited}s"
                        is VideoJob.Failed -> { status = "⛔ ${job.message}"; busy = false; return@launch }
                        is VideoJob.Ready -> {
                            val bytes = job.bytes ?: job.uri?.let { MediaAi.download(key, it).getOrElse { e -> status = "⛔ Download failed: ${e.message}"; busy = false; return@launch } }
                            if (bytes == null) { status = "⛔ The job finished without a video."; busy = false; return@launch }
                            val dir = File(context.cacheDir, "drawings").apply { mkdirs() }
                            videoFile = File(dir, "AI_illustration_video_${System.currentTimeMillis()}.mp4").apply { writeBytes(bytes) }
                            status = "Video ready (AI illustration, not to scale)."
                            busy = false; return@launch
                        }
                    }
                }
                status = "⛔ Gave up after 15 minutes; the job may still finish on Google's side."
                busy = false
            }
        }
        videoFile?.let { f -> ActionButton("Share AI video", modifier = Modifier.fillMaxWidth()) { DrawingExporter.share(context, listOf(f), "video/mp4", "AI illustration video (not to scale)") } }
        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        status?.let { Text(it, fontSize = 10.sp, color = if (it.startsWith("⛔")) AlertRed else c.textMuted) }
    }
}

/** Saves the AI picture as PNG with a band that says it is an AI illustration, not to scale. */
private fun saveLabelled(context: Context, bytes: ByteArray): File? {
    val src = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
    val band = (src.height * 0.07f).coerceAtLeast(36f)
    val out = android.graphics.Bitmap.createBitmap(src.width, src.height + band.toInt(), android.graphics.Bitmap.Config.ARGB_8888)
    val cv = android.graphics.Canvas(out)
    cv.drawColor(android.graphics.Color.BLACK)
    cv.drawBitmap(src, 0f, 0f, null)
    val p = android.graphics.Paint().apply { color = android.graphics.Color.WHITE; textSize = band * 0.45f; isAntiAlias = true; isFakeBoldText = true }
    cv.drawText("AI illustration — not to scale (AeroFast Pro)", band * 0.3f, src.height + band * 0.65f, p)
    val dir = File(context.cacheDir, "drawings").apply { mkdirs() }
    val f = File(dir, "AI_illustration_${System.currentTimeMillis()}.png")
    f.outputStream().use { out.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
    return f
}
