package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.FastenerCalculationResult
import com.example.data.GeminiAiService
import com.example.ui.theme.AlertRed
import com.example.ui.theme.LocalCustomColors
import kotlinx.coroutines.launch
import org.json.JSONObject

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val isUser: Boolean,
    val text: String,
    val attachmentBitmap: Bitmap? = null
)

@Composable
fun AiCoPilotScreen(
    currentResult: FastenerCalculationResult,
    extractedSpec: com.example.data.ExtractedFastenerSpec? = null,
    /** The Sizing selection when a scanned drawing is the primary input (currentResult is then the drawing). */
    sizingResult: FastenerCalculationResult? = null,
    onOpenScanner: () -> Unit = {},
    initialMessages: List<com.example.data.ChatMessageData> = emptyList(),
    onChatMessageSent: ((List<com.example.data.ChatMessageData>) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val customColors = LocalCustomColors.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val currentLang = com.example.util.LocalAppLanguage.current
    // Keyed only on language: keying on initialMessages rebuilt the list on every send and dropped attachments
    val messages = remember(currentLang) {
        val list = mutableStateListOf<ChatMessage>()
        if (initialMessages.isNotEmpty()) {
            initialMessages.forEach { m ->
                list.add(ChatMessage(id = m.id, isUser = m.isUser, text = m.text))
            }
        } else {
            list.add(
                ChatMessage(
                    isUser = false,
                    text = com.example.util.AppLocalization.getCoPilotGreeting(currentLang)
                )
            )
        }
        list
    }

    var inputText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var attachedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var attachedFileName by remember { mutableStateOf<String?>(null) }

    fun notifyMessagesUpdated() {
        val dataList = messages.map {
            com.example.data.ChatMessageData(id = it.id, isUser = it.isUser, text = it.text)
        }
        onChatMessageSent?.invoke(dataList)
    }

    // Speech to text launcher
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                inputText = spokenText
            }
        }
    }

    // Camera capture launcher
    var pendingCaptureUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { saved ->
        val bitmap = pendingCaptureUri?.takeIf { saved }?.let { com.example.util.CameraCaptureHelper.loadCapturedBitmap(context, it) }
        if (bitmap != null) {
            attachedBitmap = bitmap
            attachedFileName = "Camera Photo (${bitmap.width}x${bitmap.height})"
        } else if (saved) {
            Toast.makeText(context, "Could not read the camera photo", Toast.LENGTH_SHORT).show()
        }
    }

    fun launchFullResCamera() {
        val uri = com.example.util.CameraCaptureHelper.createCaptureUri(context)
        pendingCaptureUri = uri
        cameraLauncher.launch(uri)
    }

    // Safe Camera launcher with permission check
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            try {
                launchFullResCamera()
            } catch (e: Exception) {
                Toast.makeText(context, "Camera not available: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Camera permission needed to attach photo.", Toast.LENGTH_SHORT).show()
        }
    }

    fun handleCoPilotCamera() {
        val perm = androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA)
        if (perm == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            try {
                launchFullResCamera()
            } catch (e: Exception) {
                Toast.makeText(context, "Camera not available: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        } else {
            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }

    // Attachment picker supporting both Photos and PDF Blueprints
    val fileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val res = com.example.util.DocumentAttachmentHelper.loadAttachment(context, uri, pageIndex = 0)
            res.onSuccess { att ->
                attachedBitmap = att.bitmap
                attachedFileName = if (att.isPdf) "${att.fileName} (PDF Page 1)" else att.fileName
            }.onFailure { e ->
                Toast.makeText(context, "Could not open document: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun sendMessage(userText: String) {
        val trimmed = userText.trim()
        val currentBitmap = attachedBitmap
        if (trimmed.isEmpty() && currentBitmap == null) return
        if (isLoading) return

        val effectivePrompt = trimmed.ifEmpty { "Analyze attached technical blueprint / photo for fastener dimensions, thread pitch, and material alloy." }

        messages.add(
            ChatMessage(
                isUser = true,
                text = effectivePrompt,
                attachmentBitmap = currentBitmap
            )
        )
        notifyMessagesUpdated()

        inputText = ""
        attachedBitmap = null
        attachedFileName = null
        isLoading = true

        // Earlier turns (without the question just added) so the AI can follow up on its previous answers
        val history = messages.dropLast(1).map {
            com.example.data.ChatMessageData(id = it.id, isUser = it.isUser, text = it.text)
        }

        scope.launch {
            try {
                val reply = GeminiAiService.askCoPilot(
                    prompt = effectivePrompt,
                    currentResult = currentResult,
                    extractedSpec = extractedSpec,
                    sizingResult = sizingResult,
                    imageBitmap = currentBitmap,
                    context = context,
                    history = history
                )
                messages.add(ChatMessage(isUser = false, text = reply))
            } catch (e: Exception) {
                val fallback = GeminiAiService.tryAnswerLocally(trimmed, currentResult, extractedSpec)
                val replyText = fallback ?: "🤖 **AeroFast AI Co-Pilot**: Based on your setup (${currentResult.threadLabel}), pre-roll blank target is Ø${String.format("%.3f", currentResult.blankTargetDia)} mm and AMS 2759 soaking time is ${currentResult.heatTreatmentSpec.soakingTimeMinutes}."
                messages.add(ChatMessage(isUser = false, text = replyText))
            } finally {
                isLoading = false
                notifyMessagesUpdated()
                listState.animateScrollToItem(messages.size - 1)
            }
        }
    }

    fun exportCadJson() {
        val r = currentResult
        val json = JSONObject().apply {
            put("tool", "AeroFast Pro Thread Master")
            put("timestamp", System.currentTimeMillis())
            put("fastener_type", r.fastenerType.label)
            put("standard", r.standard.label)
            put("thread_label", r.threadLabel)
            put("nominal_dia_mm", r.nominalDiaMm)
            put("pitch_mm", r.pitchMm)
            put("length_mm", r.lengthMm)
            put("thread_length_mm", r.threadLengthMm)
            put("material_name", r.material.name)
            put("material_uts_mpa", r.material.uts)
            put("material_km", r.material.km)
            put("coating", r.coating.label)
            put("turning_blank_dia_mm", r.blankTargetDia)
            put("blank_tolerance_half_mm", r.blankTolHalf)
            put("blank_upper_limit_mm", r.blankUpperLimit)
            put("blank_lower_limit_mm", r.blankLowerLimit)
            put("pitch_sweet_spot_mm", r.pitchDiaSweet)
            put("pitch_min_mm", r.pitchDiaMin)
            put("pitch_max_mm", r.pitchDiaMax)
            put("forging_tonnage", r.forgingTonnage)
            put("recommended_machine", r.recommendedMachine)
            put("hydrogen_embrittlement_risk", r.hasHydrogenEmbrittlementRisk)
            put("unit_price", r.finalUnitPrice)
            put("currency", r.currencySymbol)
        }

        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, json.toString(2))
            type = "application/json"
        }
        context.startActivity(Intent.createChooser(sendIntent, "Export AeroFast Spec JSON"))
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(14.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f, fill = false)
            ) {
                Icon(
                    Icons.Default.Psychology,
                    contentDescription = "AI CoPilot",
                    tint = customColors.accentPrimary,
                    modifier = Modifier.padding(end = 6.dp)
                )
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text(
                        text = "AERO METALLURGY CO-PILOT",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Black,
                        color = customColors.textMain,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (extractedSpec != null) "Drawing: ${currentResult.threadLabel} ${currentResult.fastenerType.label.substringBefore(" (")} • ${"%,.0f".format(java.util.Locale.US, com.example.data.DrawingScan.volume(extractedSpec, currentResult).totalMm3)} mm³"
                            else "No drawing scanned — tap to open the AI Scanner. Using Sizing: ${currentResult.threadLabel} (${currentResult.material.name})",
                        modifier = Modifier.clickable(enabled = extractedSpec == null) { onOpenScanner() },
                        fontSize = 9.5.sp,
                        color = customColors.accentSecondary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // New Chat Button
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = customColors.accentPrimary,
                    modifier = Modifier.clickable {
                        messages.clear()
                        messages.add(
                            ChatMessage(
                                isUser = false,
                                text = com.example.util.AppLocalization.getCoPilotGreeting(currentLang)
                            )
                        )
                        inputText = ""
                        attachedBitmap = null
                        attachedFileName = null
                        notifyMessagesUpdated()
                        Toast.makeText(context, "New AI Co-Pilot chat session started.", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "New Chat",
                            tint = Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = com.example.util.AppLocalization.t("new_chat", currentLang),
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                }

                // Refresh Button
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = customColors.bgElevated,
                    border = androidx.compose.foundation.BorderStroke(1.dp, customColors.border),
                    modifier = Modifier.clickable {
                        notifyMessagesUpdated()
                        Toast.makeText(context, "AI Co-Pilot chat refreshed & synced.", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Box(modifier = Modifier.padding(6.dp)) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh Chat",
                            tint = customColors.accentPrimary,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }

                // Export CAD JSON Button
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = customColors.bgElevated,
                    border = androidx.compose.foundation.BorderStroke(1.dp, customColors.border),
                    modifier = Modifier.clickable { exportCadJson() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Download,
                            contentDescription = "Export JSON",
                            tint = customColors.accentPrimary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "CAD JSON",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = customColors.textMain
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Quick Suggestion Chips (Horizontal scroll)
        val chipsScroll = rememberScrollState()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(chipsScroll),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val suggestions = listOf(
                "🛡️ Poka-Yoke De-embrittlement Baking Protocol (ASTM F1940)",
                "🛡️ Poka-Yoke VDI 2230 Bolted Joint Over-Torque Safeguard",
                "🛡️ Poka-Yoke ISO 1502 GO/NO-GO Ring Gauge Limits",
                "🛡️ Poka-Yoke CNC G-Code Collision-Free Threading",
                "Check list of materials & supported alloys",
                "Inspect blueprint callouts (OD, ID, tolerances)",
                "Heading die: ASP30 vs GT55?",
                "Turning blank formula explanation?"
            )
            suggestions.forEach { suggestion ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = customColors.bgElevated,
                    border = androidx.compose.foundation.BorderStroke(1.dp, customColors.border),
                    modifier = Modifier.clickable { sendMessage(suggestion) }
                ) {
                    Text(
                        text = suggestion,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = customColors.accentPrimary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Chat Message List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                ChatBubble(message = msg)
            }

            if (isLoading) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        CircularProgressIndicator(
                            color = customColors.accentPrimary,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "AeroFast Co-Pilot thinking...",
                            fontSize = 11.sp,
                            color = customColors.textMuted
                        )
                    }
                }
            }
        }

        // Active Attachment Bar
        if (attachedFileName != null) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = customColors.accentSecondary.copy(alpha = 0.15f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        attachedBitmap?.let { bmp ->
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "Attached thumbnail",
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(
                            text = attachedFileName ?: "Attached file",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = customColors.accentSecondary
                        )
                    }

                    IconButton(
                        onClick = {
                            attachedBitmap = null
                            attachedFileName = null
                        },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Remove",
                            tint = AlertRed,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Bottom Input Row: Mic, Camera, File, Text, Send
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = customColors.bgCard,
            border = androidx.compose.foundation.BorderStroke(1.dp, customColors.border),
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 4.dp)
        ) {
            Row(
                modifier = Modifier.padding(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Speech Input
                IconButton(
                    onClick = {
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your fastener question...")
                        }
                        try {
                            speechLauncher.launch(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Voice input not supported on this device.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.Mic,
                        contentDescription = "Microphone",
                        tint = AlertRed,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Camera Photo
                IconButton(
                    onClick = { handleCoPilotCamera() },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = "Camera",
                        tint = customColors.accentSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Attach File (Photo or PDF)
                IconButton(
                    onClick = { fileLauncher.launch(arrayOf("application/pdf", "image/*")) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.AttachFile,
                        contentDescription = "Attach Photo or PDF",
                        tint = customColors.accentPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Input Field
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text(com.example.util.AppLocalization.t("ask_copilot_placeholder", currentLang), fontSize = 12.sp, color = customColors.textMuted) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Send
                    ),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (!isLoading && (inputText.isNotBlank() || attachedBitmap != null)) {
                                sendMessage(inputText)
                            }
                        }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent
                    ),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontSize = 12.sp,
                        color = customColors.textMain
                    ),
                    modifier = Modifier.weight(1f)
                )

                // Send Button
                Surface(
                    shape = CircleShape,
                    color = if (isLoading || (inputText.isBlank() && attachedBitmap == null)) {
                        customColors.accentPrimary.copy(alpha = 0.4f)
                    } else {
                        customColors.accentPrimary
                    },
                    modifier = Modifier
                        .size(38.dp)
                        .clickable(enabled = !isLoading && (inputText.isNotBlank() || attachedBitmap != null)) {
                            sendMessage(inputText)
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(message: ChatMessage) {
    val customColors = LocalCustomColors.current
    val isUser = message.isUser

    val displayText = remember(message.text, isUser) {
        if (isUser) androidx.compose.ui.text.AnnotatedString(message.text)
        else renderBasicMarkdown(cleanAiMarkdownText(message.text))
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 12.dp,
                topEnd = 12.dp,
                bottomStart = if (isUser) 12.dp else 2.dp,
                bottomEnd = if (isUser) 2.dp else 12.dp
            ),
            color = if (isUser) customColors.accentPrimary else customColors.bgElevated,
            border = if (isUser) null else androidx.compose.foundation.BorderStroke(1.dp, customColors.border),
            modifier = Modifier.fillMaxWidth(0.88f)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = if (isUser) "You" else "Co-Pilot",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isUser) Color.White.copy(alpha = 0.8f) else customColors.accentPrimary
                )

                message.attachmentBitmap?.let { bmp ->
                    Spacer(modifier = Modifier.height(6.dp))
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "Attached photo",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = displayText,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    fontFamily = FontFamily.Default,
                    color = if (isUser) Color.White else customColors.textMain
                )
            }
        }
    }
}

private fun cleanAiMarkdownText(raw: String): String {
    var cleaned = raw
    // Remove internal thoughts if present
    cleaned = cleaned.replace(Regex("""(?s)<thought>.*?</thought>"""), "")
    // Remove headers like ### or ## or #
    cleaned = cleaned.replace(Regex("""(^|\n)(#{1,6})\s*"""), "$1")
    // Clean LaTeX math symbols like $\r_\varepsilon = 0.2\text{ mm}$ -> r_ε = 0.2 mm
    cleaned = cleaned.replace(Regex("""\$([^$]+)\$""")) { matchResult ->
        var math = matchResult.groupValues[1]
        math = math.replace(Regex("""\\text\{([^}]+)\}"""), "$1")
        math = math.replace(Regex("""\\r_\\varepsilon"""), "r_ε")
        math = math.replace(Regex("""\\varepsilon"""), "ε")
        math = math.replace(Regex("""\\"""), "")
        math
    }
    return cleaned.trim()
}

// Turns the AI's markdown into styled text: "* item" / "- item" become bullets, **bold** is bolded, other markers are dropped
private fun renderBasicMarkdown(text: String): androidx.compose.ui.text.AnnotatedString {
    val bold = androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold)
    val lines = text.lines().map { line ->
        line.replace(Regex("""^(\s*)[*\-+]\s+"""), "$1• ")
    }
    return androidx.compose.ui.text.buildAnnotatedString {
        lines.forEachIndexed { i, line ->
            val segments = line.split("**")
            segments.forEachIndexed { s, seg ->
                val plain = seg.replace("__", "").replace("`", "")
                    .replace(Regex("""(?<![\w*])[*_](\S[^*_]*?)[*_](?![\w*])"""), "$1")
                if (s % 2 == 1 && s < segments.size - 1) {
                    pushStyle(bold); append(plain); pop()
                } else {
                    append(plain)
                }
            }
            if (i < lines.size - 1) append('\n')
        }
    }
}
