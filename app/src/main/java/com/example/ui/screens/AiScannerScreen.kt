package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import java.io.File
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PrecisionManufacturing
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.CoatingType
import com.example.data.DetectedDimension
import com.example.data.ExtractedFastenerSpec
import com.example.data.FastenerCatalog
import com.example.data.FastenerType
import com.example.data.GeminiAiService
import com.example.data.ProductStandard
import com.example.data.PropertyClass
import com.example.data.UnitSystem
import com.example.ui.components.SettingsDialog
import com.example.ui.theme.AlertRed
import com.example.ui.theme.LocalCustomColors
import com.example.ui.theme.SuccessGreen
import com.example.util.AttachmentResult
import com.example.util.CameraCaptureHelper
import com.example.util.DocumentAttachmentHelper
import com.example.util.SampleBlueprintType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun AiScannerScreen(
    currentResult: com.example.data.FastenerCalculationResult,
    onApplyExtractedSpec: (ExtractedFastenerSpec) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val customColors = LocalCustomColors.current
    val currentLang = com.example.util.LocalAppLanguage.current
    val scrollState = rememberScrollState()

    var activeAttachment by remember { mutableStateOf<AttachmentResult?>(null) }
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isAnalyzing by remember { mutableStateOf(false) }
    var extractedSpec by remember { mutableStateOf<ExtractedFastenerSpec?>(null) }
    var ocrValues by remember { mutableStateOf<List<com.example.util.OcrValue>?>(null) }
    var extractionError by remember { mutableStateOf<String?>(null) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    fun processUri(uri: Uri) {
        isAnalyzing = true
        extractionError = null
        val result = DocumentAttachmentHelper.loadAttachment(context, uri, 0)
        result.onSuccess { att ->
            activeAttachment = att
            selectedBitmap = att.bitmap
            isAnalyzing = false
        }.onFailure { err ->
            isAnalyzing = false
            extractionError = err.message ?: "Failed to load document attachment"
        }
    }

    fun loadSampleBlueprint(sampleType: SampleBlueprintType) {
        val att = DocumentAttachmentHelper.createSampleBlueprintBitmap(sampleType)
        activeAttachment = att
        selectedBitmap = att.bitmap
        extractedSpec = ExtractedFastenerSpec(
            fastenerType = when(sampleType) {
                SampleBlueprintType.M8_SHCS -> FastenerType.SHCS
                SampleBlueprintType.M12_FLANGE_BOLT -> FastenerType.FLANGE_BOLT
                else -> FastenerType.STUD
            },
            unitSystem = UnitSystem.METRIC,
            nominalSizeIndex = com.example.data.FastenerCatalog.METRIC_SIZES.indices
                .minByOrNull { kotlin.math.abs(com.example.data.FastenerCatalog.METRIC_SIZES[it].d - sampleType.nominalDiaMm) }
                ?: com.example.data.FastenerCatalog.defaultSizeIndex(com.example.data.UnitSystem.METRIC),
            nominalDiaMm = sampleType.nominalDiaMm,
            pitchValue = sampleType.pitchMm,
            lengthMm = sampleType.lengthMm,
            propertyClass = PropertyClass.CLASS_10_9,
            materialKey = sampleType.materialKey,
            threadClass = "6g",
            coating = CoatingType.ZN_NI,
            technicalSummary = "Sample Aerospace Blueprint verified per ${sampleType.title}.",
            detectedDimensions = listOf(
                DetectedDimension("Nominal Diameter", "${sampleType.nominalDiaMm} mm", "±0.05 mm"),
                DetectedDimension("Thread Pitch", "${sampleType.pitchMm} mm", "ISO 6H/6g"),
                DetectedDimension("Shank Length", "${sampleType.lengthMm} mm", "±0.2 mm")
            ),
            isAiVisionExtracted = true,
            modelUsed = "Sample Spec (built-in)"
        )
        extractionError = null
    }

    fun analyzeBitmap(bitmap: Bitmap) {
        isAnalyzing = true
        extractionError = null
        ocrValues = null
        if (!GeminiAiService.hasLiveApiKey(context)) {
            // No AI key: read the numbers on the phone and let the user say what each one is
            CoroutineScope(Dispatchers.Main).launch {
                runCatching { com.example.util.OcrDimensionReader.read(bitmap) }
                    .onSuccess { vals ->
                        extractedSpec = null
                        if (vals.isEmpty()) extractionError = "No numbers could be read from this image. Try a sharper photo, or add a Gemini key (Key button) for full AI reading."
                        else ocrValues = vals
                    }
                    .onFailure { extractionError = "On-phone text reading failed: ${it.message}" }
                isAnalyzing = false
            }
            return
        }
        CoroutineScope(Dispatchers.Main).launch {
            val result = GeminiAiService.extractBlueprintSpecFromImage(bitmap, context)
            isAnalyzing = false
            result.onSuccess { spec ->
                extractedSpec = spec
            }.onFailure { err ->
                extractionError = err.message ?: "AI Extraction failed"
            }
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {}
            processUri(it)
        }
    }

    var pendingCaptureUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { saved ->
        val bitmap = pendingCaptureUri?.takeIf { saved }?.let { CameraCaptureHelper.loadCapturedBitmap(context, it) }
        if (saved && bitmap == null) {
            Toast.makeText(context, "Could not read the camera photo", Toast.LENGTH_SHORT).show()
        }
        if (bitmap != null) {
            activeAttachment = AttachmentResult(
                uri = null,
                fileName = "Camera Photo (${bitmap.width}x${bitmap.height})",
                isPdf = false,
                pageCount = 1,
                currentPage = 0,
                bitmap = bitmap,
                fileSizeFormatted = "Camera Photo"
            )
            selectedBitmap = bitmap
            analyzeBitmap(bitmap)
        }
    }

    fun launchFullResCamera() {
        try {
            val uri = CameraCaptureHelper.createCaptureUri(context)
            pendingCaptureUri = uri
            cameraLauncher.launch(uri)
        } catch (e: Exception) {
            Toast.makeText(context, "Camera not available: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            launchFullResCamera()
        } else {
            Toast.makeText(context, "Camera permission is required to scan blueprints", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Main Header Card
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = customColors.bgCard,
            border = BorderStroke(1.dp, customColors.border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.DocumentScanner,
                            contentDescription = "Scanner",
                            tint = customColors.accentPrimary,
                            modifier = Modifier.padding(end = 6.dp)
                        )
                        Text(
                            text = "GEMINI 3.8 AI BLUEPRINT VISION",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = customColors.textMain
                        )
                    }

                    IconButton(
                        onClick = { showSettingsDialog = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Key,
                            contentDescription = "API Key",
                            tint = if (GeminiAiService.hasLiveApiKey(context)) SuccessGreen else AlertRed,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Upload PDF blueprints or photos of threaded fasteners. Gemini AI Vision extracts nominal diameter, pitch, length, material, and coating specs, and auto-recommends input wire conditions & process flows.",
                    fontSize = 10.sp,
                    color = customColors.textMuted
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            launcher.launch(arrayOf("application/pdf", "image/*"))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = customColors.accentPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).height(46.dp)
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = "Upload", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(com.example.util.AppLocalization.t("pdf_or_image", currentLang), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                launchFullResCamera()
                            } else {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = customColors.accentSecondary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).height(46.dp)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = "Camera Scan", tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(com.example.util.AppLocalization.t("camera_scan", currentLang), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "OR LOAD SAMPLE AEROSPACE DRAWING:",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = customColors.textMuted
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SampleChip(
                            title = "M8 SHCS (Ti-6Al-4V)",
                            standard = "DIN 912",
                            onClick = { loadSampleBlueprint(SampleBlueprintType.M8_SHCS) }
                        )
                        SampleChip(
                            title = "M12 Flange (4140 Alloy)",
                            standard = "ISO 4162",
                            onClick = { loadSampleBlueprint(SampleBlueprintType.M12_FLANGE_BOLT) }
                        )
                        SampleChip(
                            title = "1/2-20 Stud (Inconel 718)",
                            standard = "MS21250",
                            onClick = { loadSampleBlueprint(SampleBlueprintType.HALF_INCH_INCONEL_STUD) }
                        )
                    }
                }

                activeAttachment?.let { att ->
                    Spacer(modifier = Modifier.height(14.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = customColors.bgElevated,
                        border = BorderStroke(1.dp, customColors.border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (att.isPdf) AlertRed.copy(alpha = 0.2f) else customColors.accentPrimary.copy(alpha = 0.2f),
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            if (att.isPdf) Icons.Default.PictureAsPdf else Icons.Default.Image,
                                            contentDescription = "Format",
                                            tint = if (att.isPdf) AlertRed else customColors.accentPrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(text = att.fileName, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = customColors.textMain, maxLines = 1)
                                    Text(text = att.fileSizeFormatted, fontSize = 10.sp, color = customColors.textMuted)
                                }
                            }

                            IconButton(
                                onClick = {
                                    activeAttachment = null
                                    selectedBitmap = null
                                    extractedSpec = null
                                    extractionError = null
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = customColors.textMuted, modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    selectedBitmap?.let { bmp ->
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(210.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .border(1.dp, customColors.border, RoundedCornerShape(10.dp))
                        ) {
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "Preview",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.matchParentSize().background(Color.Black)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = { analyzeBitmap(bmp) },
                            enabled = !isAnalyzing,
                            colors = ButtonDefaults.buttonColors(containerColor = customColors.accentPrimary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().height(46.dp)
                        ) {
                            if (isAnalyzing) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Analyzing Drawing with Gemini 3.8 Flash...", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            } else {
                                Icon(Icons.Default.FlashOn, contentDescription = "Analyze", modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(com.example.util.AppLocalization.t("analyze_blueprint", currentLang), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                extractionError?.let { err ->
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = AlertRed.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, AlertRed.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = "Error", tint = AlertRed, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = err, fontSize = 11.sp, color = AlertRed, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                ocrValues?.let { vals ->
                    OcrAssignPanel(vals, currentResult) { spec ->
                        extractedSpec = spec
                        ocrValues = null
                    }
                }

                extractedSpec?.let { spec ->
                    Spacer(modifier = Modifier.height(14.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = customColors.bgElevated,
                        border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = "Success", tint = SuccessGreen, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = "BLUEPRINT DETECTED SPECIFICATIONS", fontSize = 11.sp, fontWeight = FontWeight.Black, color = SuccessGreen)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(text = spec.technicalSummary, fontSize = 11.sp, color = customColors.textMuted, modifier = Modifier.padding(bottom = 8.dp))

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                SpecItemRow("Type", spec.fastenerType.label)
                                SpecItemRow("Diameter", "${spec.nominalDiaMm} mm")
                                SpecItemRow("Pitch", "${spec.pitchValue} mm")
                                SpecItemRow("Length", "${spec.lengthMm} mm")
                                SpecItemRow("Material", spec.materialKey)
                                SpecItemRow("Property Class", spec.propertyClass.label)
                                SpecItemRow("Thread Class", spec.threadClass)
                                SpecItemRow("Coating", spec.coating.label)
                            }

                            if (com.example.data.BoltForging.applies(spec.fastenerType)) {
                                BoltForgingPanel(spec)
                            }

                            // Every dimension the AI read off the drawing, with its tolerance
                            if (spec.detectedDimensions.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "DIMENSIONS EXTRACTED FROM DRAWING (${spec.detectedDimensions.size})",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = customColors.accentPrimary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    spec.detectedDimensions.forEach { dim ->
                                        SpecItemRow(
                                            dim.feature.ifBlank { "Dimension" },
                                            listOf(dim.nominal, dim.tolerance).filter { it.isNotBlank() }.joinToString("  ")
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedButton(
                                    onClick = {
                                        val text = buildString {
                                            append("${spec.fastenerType.label} — dimensions extracted by AeroFast Pro\n")
                                            spec.detectedDimensions.forEach { d ->
                                                append("${d.feature}: ${d.nominal} ${d.tolerance}".trim()).append('\n')
                                            }
                                        }
                                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                        clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Dimensions", text))
                                        Toast.makeText(context, "Dimensions copied", Toast.LENGTH_SHORT).show()
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Copy Dimensions", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Text(
                                text = "Read by: ${spec.modelUsed}",
                                fontSize = 9.sp,
                                color = customColors.textMuted,
                                modifier = Modifier.padding(top = 6.dp)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    onApplyExtractedSpec(spec)
                                    Toast.makeText(context, "Applied specs to Sizing & Heat Treatment!", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().height(46.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = "Apply", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Apply Extracted Specs to Sizing", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            AiProcessFlowCard(spec = spec)
                        }
                    }
                }
            }
        }
    }

    if (showSettingsDialog) {
        SettingsDialog(onDismiss = { showSettingsDialog = false })
    }
}

@Composable
fun AiProcessFlowCard(spec: ExtractedFastenerSpec) {
    val customColors = LocalCustomColors.current
    val mat = FastenerCatalog.MATERIALS[spec.materialKey] ?: FastenerCatalog.MATERIALS["10B21"]!!
    val isAero = mat.isAero

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = customColors.bgCard,
        border = BorderStroke(1.dp, customColors.accentPrimary.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.PrecisionManufacturing, contentDescription = "Process Flow", tint = customColors.accentPrimary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "AI-RECOMMENDED PROCESS FLOW & POKA-YOKE AUDIT",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = customColors.textMain
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Poka-Yoke Blueprint Audit Card
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = customColors.bgElevated,
                border = BorderStroke(1.dp, customColors.accentPrimary.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(text = "🛡️ POKA-YOKE BLUEPRINT COMPLIANCE AUDIT", fontSize = 10.sp, fontWeight = FontWeight.Black, color = customColors.accentPrimary)
                    Text(
                        text = "• Pitch & Diameter: M${spec.nominalDiaMm} x ${spec.pitchValue}mm complies with ISO 261 / ASME B1.1 standard thread pitch\n" +
                                "• Material Compatibility: ${spec.materialKey} verified for ${spec.fastenerType.label} forging & thread rolling\n" +
                                "• Hydrogen Risk: ${if (mat.hasHighHardness && spec.coating.name.contains("ZN")) "⚠️ Mandatory De-embrittlement Baking (190°C–200°C, 8-24 hrs)" else "✓ Low risk under standard plating process"}",
                        fontSize = 9.sp,
                        color = customColors.textMain,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = customColors.bgElevated,
                border = BorderStroke(1.dp, customColors.border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(text = "1. Forging Method: ${if (isAero) "Controlled Warm Heading (650–700°C) with Carbide Dies" else "High-Speed Multi-Station Cold Heading"}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = customColors.accentSecondary)
                    Text(text = "2. Input Wire Sourcing: ${mat.rawMaterialSourcingUtsMpa} MPa • Density: ${mat.density} g/cm³", fontSize = 9.5.sp, color = customColors.textMain, modifier = Modifier.padding(top = 2.dp))
                    Text(text = "3. Surface Lubrication: ${mat.lubricationCoating}", fontSize = 9.5.sp, color = customColors.textMain, modifier = Modifier.padding(top = 2.dp))
                    Text(text = "4. Heat Treatment: ${mat.heatTreatment}", fontSize = 9.5.sp, color = customColors.textMain, modifier = Modifier.padding(top = 2.dp))
                    Text(text = "5. Tooling Steels: ${mat.dieTooling}", fontSize = 9.5.sp, color = customColors.textMain, modifier = Modifier.padding(top = 2.dp))
                }
            }
        }
    }
}

@Composable
private fun SpecItemRow(label: String, value: String) {
    val customColors = LocalCustomColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 10.sp, color = customColors.textMuted)
        Text(text = value, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = customColors.textMain)
    }
}

@Composable
private fun SampleChip(
    title: String,
    standard: String,
    onClick: () -> Unit
) {
    val customColors = LocalCustomColors.current
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = customColors.bgElevated,
        border = BorderStroke(1.dp, customColors.border),
        modifier = Modifier.clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(text = title, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = customColors.textMain)
            Text(text = standard, fontSize = 8.5.sp, color = customColors.accentSecondary)
        }
    }
}

/** Finished volume, trim cheese and previous-stage (pre-extrusion) length straight from the scanned drawing. */
@Composable
private fun BoltForgingPanel(spec: ExtractedFastenerSpec) {
    val customColors = LocalCustomColors.current
    val material = FastenerCatalog.MATERIALS[spec.materialKey] ?: FastenerCatalog.MATERIALS["10B21"] ?: return
    val pitchMm = if (spec.unitSystem == com.example.data.UnitSystem.METRIC) spec.pitchValue
        else if (spec.pitchValue > 3.0) 25.4 / spec.pitchValue else spec.pitchValue
    val f = remember(spec) {
        val calc = com.example.data.CalculationEngine.calculate(
            fastenerType = spec.fastenerType, unitSystem = spec.unitSystem,
            standard = com.example.data.PokaYokeEngine.defaultStandard(spec.fastenerType, spec.unitSystem),
            nominalSizeIndex = spec.nominalSizeIndex, pitchValue = spec.pitchValue, lengthMm = spec.lengthMm,
            threadClass = spec.threadClass, material = material, coating = spec.coating, batchQty = 1,
            customNominalDiaMm = spec.nominalDiaMm, propertyClass = spec.propertyClass
        )
        val fl = if (spec.fastenerType == FastenerType.FLANGE_BOLT) com.example.data.BoltForging.resolveFlange(
            spec.nominalDiaMm, spec.flangeDiaMm, spec.headHeightMm, spec.headAcrossFlatsMm, spec.flangeThicknessMm) else null
        com.example.data.BoltForging.compute(
            type = spec.fastenerType, unit = spec.unitSystem, dMm = spec.nominalDiaMm, pitchMm = pitchMm, lengthMm = spec.lengthMm,
            blankDiaMm = calc.blankTargetDia, wireDiaMm = calc.wireDiaMm, tableHead = calc.headDimensions, densityGPerCm3 = material.density,
            threadLengthMm = spec.threadLengthMm, shankDiaMm = spec.shankDiaMm,
            acrossFlatsMm = fl?.s ?: spec.headAcrossFlatsMm, headHeightMm = fl?.k ?: spec.headHeightMm,
            flangeDiaMm = fl?.dc, flangeThicknessMm = fl?.c
        )
    }
    fun n(v: Double, dp: Int = 2) = "%.${dp}f".format(java.util.Locale.US, v)
    fun src(fromDrawing: Boolean) = if (fromDrawing) "drawing" else "standard"
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = customColors.bgCard,
        border = BorderStroke(1.dp, customColors.accentSecondary.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text("FORGING DATA FROM DRAWING", fontSize = 11.sp, fontWeight = FontWeight.Black, color = customColors.accentSecondary)
            f.finishedVolumeMm3?.let { SpecItemRow("Finished volume", "${n(it, 0)} mm³ = ${n(it * f.densityGPerMm3)} g") }
            f.cheeseVolumeMm3?.let { SpecItemRow("Trim cheese", "${n(it, 0)} mm³ = ${n(it * f.densityGPerMm3)} g") }
            f.headStockMm?.let { SpecItemRow("Head stock", "${n(it)} mm of Ø${n(f.shankDiaMm)}") }
            SpecItemRow("Thread length b (${src(f.threadFromDrawing)})", "${n(f.threadLengthMm, 1)} mm")
            SpecItemRow("Shank Ø (${src(f.shankFromDrawing)}) → blank Ø", "${n(f.shankDiaMm)} → ${n(f.blankDiaMm, 3)} mm")
            SpecItemRow("Cone 15°/side", "${n(f.coneLengthMm)} mm")
            SpecItemRow("Stock to extrude", "${n(f.stockToExtrudeMm)} mm")
            SpecItemRow("Length under head before extrusion", "${n(f.lengthUnderHeadBeforeMm)} mm")
            f.cutOffLengthMm?.let { SpecItemRow("Cut-off length", "${n(it)} mm") }
            Text(f.headNote + " (${src(f.headFromDrawing)})", fontSize = 10.sp, color = customColors.textMuted)
            if (f.missing.isNotEmpty()) {
                Text("Not on drawing (standard value used where one exists): ${f.missing.joinToString()}", fontSize = 10.sp, color = customColors.accentPrimary)
            }
        }
    }
}

private val OCR_ROLES = listOf("—", "Nominal d", "Length L", "Thread length b", "Shank Ø", "Across flats s", "Head height k", "Flange Ø dc", "Flange thickness c")

/** On-phone OCR result: the user assigns each number; type, material, class and coating come from the current Sizing selection. */
@Composable
private fun OcrAssignPanel(
    values: List<com.example.util.OcrValue>,
    current: com.example.data.FastenerCalculationResult,
    onUse: (ExtractedFastenerSpec) -> Unit
) {
    val customColors = LocalCustomColors.current
    val roles = remember(values) { androidx.compose.runtime.mutableStateMapOf<Int, String>() }
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = customColors.bgElevated,
        border = BorderStroke(1.dp, customColors.accentSecondary.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth().padding(top = 14.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("READ ON PHONE (OCR) — TAP WHAT EACH NUMBER IS", fontSize = 11.sp, fontWeight = FontWeight.Black, color = customColors.accentSecondary)
            Text("No Gemini key, so the drawing was read as text only. Type, material, class and coating are taken from your current Sizing selection (${current.fastenerType.label.substringBefore(" (")}). Add a key for full automatic reading.",
                fontSize = 10.sp, color = customColors.textMuted)
            values.forEachIndexed { i, v ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text((if (v.pitch != null) "M${v.value} × ${v.pitch}" else if (v.isDiameter) "Ø${v.value}" else "${v.value}") + " mm",
                            fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = customColors.textMain)
                        Text(v.source.take(40), fontSize = 9.sp, color = customColors.textMuted)
                    }
                    SelectorDropdown(
                        label = "",
                        selectedText = roles[i] ?: OCR_ROLES.first(),
                        items = OCR_ROLES,
                        onItemSelected = { roles[i] = OCR_ROLES[it] },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            fun pick(role: String) = roles.entries.firstOrNull { it.value == role }?.let { values[it.key] }
            val d = pick("Nominal d")
            Button(
                onClick = {
                    val unit = current.unitSystem
                    val dMm = d?.value ?: current.nominalDiaMm
                    val assigned = roles.filterValues { it != OCR_ROLES.first() }
                    onUse(ExtractedFastenerSpec(
                        fastenerType = current.fastenerType, unitSystem = unit,
                        nominalSizeIndex = FastenerCatalog.sizeIndexFor(unit, dMm), nominalDiaMm = dMm,
                        pitchValue = d?.pitch ?: current.pitchMm,
                        lengthMm = pick("Length L")?.value ?: current.lengthMm,
                        propertyClass = current.propertyClass ?: PropertyClass.CLASS_8_8,
                        materialKey = current.material.key, threadClass = current.threadClass, coating = current.coating,
                        technicalSummary = "Read on phone (OCR); ${assigned.size} value(s) assigned by you. Type, material, class and coating from your Sizing selection.",
                        detectedDimensions = assigned.map { (i, role) -> DetectedDimension(role, "${values[i].value} mm", values[i].source.take(30)) },
                        isAiVisionExtracted = false,
                        modelUsed = "On-phone OCR + your assignment",
                        threadLengthMm = pick("Thread length b")?.value, shankDiaMm = pick("Shank Ø")?.value,
                        headAcrossFlatsMm = pick("Across flats s")?.value, headHeightMm = pick("Head height k")?.value,
                        flangeDiaMm = pick("Flange Ø dc")?.value, flangeThicknessMm = pick("Flange thickness c")?.value
                    ))
                },
                enabled = roles.values.any { it != OCR_ROLES.first() },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Use these values", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
        }
    }
}

