package com.aistudio.aerofast.trmst.ui.screens

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
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
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
import com.aistudio.aerofast.trmst.data.CoatingType
import com.aistudio.aerofast.trmst.data.DetectedDimension
import com.aistudio.aerofast.trmst.data.ExtractedFastenerSpec
import com.aistudio.aerofast.trmst.data.FastenerCatalog
import com.aistudio.aerofast.trmst.data.FastenerType
import com.aistudio.aerofast.trmst.data.GeminiAiService
import com.aistudio.aerofast.trmst.data.ProductStandard
import com.aistudio.aerofast.trmst.data.PropertyClass
import com.aistudio.aerofast.trmst.data.UnitSystem
import com.aistudio.aerofast.trmst.ui.components.SettingsDialog
import com.aistudio.aerofast.trmst.ui.theme.AlertRed
import com.aistudio.aerofast.trmst.ui.theme.LocalCustomColors
import com.aistudio.aerofast.trmst.ui.theme.SuccessGreen
import com.aistudio.aerofast.trmst.util.AttachmentResult
import com.aistudio.aerofast.trmst.util.CameraCaptureHelper
import com.aistudio.aerofast.trmst.util.DocumentAttachmentHelper
import com.aistudio.aerofast.trmst.util.SampleBlueprintType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun AiScannerScreen(
    currentResult: com.aistudio.aerofast.trmst.data.FastenerCalculationResult,
    onApplyExtractedSpec: (ExtractedFastenerSpec) -> Unit,
    modifier: Modifier = Modifier,
    /** The last scan, kept by the caller so it survives tab changes and feeds the Co-Pilot. */
    scannedSpec: ExtractedFastenerSpec? = null,
    onScanned: (ExtractedFastenerSpec?) -> Unit = {},
    onAskCoPilot: () -> Unit = {}
) {
    val context = LocalContext.current
    val customColors = LocalCustomColors.current
    val currentLang = com.aistudio.aerofast.trmst.util.LocalAppLanguage.current
    val scrollState = rememberScrollState()

    var activeAttachment by remember { mutableStateOf<AttachmentResult?>(null) }
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isAnalyzing by remember { mutableStateOf(false) }
    val extractedSpec = scannedSpec
    var ocrValues by remember { mutableStateOf<List<com.aistudio.aerofast.trmst.util.OcrValue>?>(null) }
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
        onScanned(ExtractedFastenerSpec(
            fastenerType = when(sampleType) {
                SampleBlueprintType.M8_SHCS -> FastenerType.SHCS
                SampleBlueprintType.M12_FLANGE_BOLT -> FastenerType.FLANGE_BOLT
                else -> FastenerType.STUD
            },
            unitSystem = UnitSystem.METRIC,
            nominalSizeIndex = com.aistudio.aerofast.trmst.data.FastenerCatalog.METRIC_SIZES.indices
                .minByOrNull { kotlin.math.abs(com.aistudio.aerofast.trmst.data.FastenerCatalog.METRIC_SIZES[it].d - sampleType.nominalDiaMm) }
                ?: com.aistudio.aerofast.trmst.data.FastenerCatalog.defaultSizeIndex(com.aistudio.aerofast.trmst.data.UnitSystem.METRIC),
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
        ))
        extractionError = null
    }

    fun analyzeBitmap(bitmap: Bitmap) {
        isAnalyzing = true
        extractionError = null
        ocrValues = null
        if (!GeminiAiService.hasLiveApiKey(context)) {
            // No AI key: read the numbers on the phone and let the user say what each one is
            CoroutineScope(Dispatchers.Main).launch {
                runCatching { com.aistudio.aerofast.trmst.util.OcrDimensionReader.read(bitmap) }
                    .onSuccess { vals ->
                        onScanned(null)
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
                onScanned(spec)
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
                            text = "AI DRAWING SCANNER — VOLUME AS PER DRAWING",
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
                    text = "Upload a PDF or a photo of the part drawing. The scanner reads every dimension and gives the total volume of the part as drawn, with its weight and cut-off length. A dimension the drawing does not show is listed with the value used instead — never guessed silently.",
                    fontSize = 10.sp,
                    color = customColors.textMuted
                )

                Spacer(modifier = Modifier.height(10.dp))
                ShopDefaultsCard()

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
                        Text(com.aistudio.aerofast.trmst.util.AppLocalization.t("pdf_or_image", currentLang), fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
                        Text(com.aistudio.aerofast.trmst.util.AppLocalization.t("camera_scan", currentLang), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
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
                                    onScanned(null)
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
                                Text("Reading the drawing...", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            } else {
                                Icon(Icons.Default.FlashOn, contentDescription = "Analyze", modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(com.aistudio.aerofast.trmst.util.AppLocalization.t("analyze_blueprint", currentLang), fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
                        onScanned(spec)
                        ocrValues = null
                    }
                }

                extractedSpec?.let { spec ->
                    // One calculation of the scanned part: the volume card, the forging data and the Co-Pilot all read it
                    val batch = com.aistudio.aerofast.trmst.data.ShopDefaults.batchQty
                    val scanResult = remember(spec, FastenerCatalog.MATERIALS.size, batch) { com.aistudio.aerofast.trmst.data.DrawingScan.calculate(spec) }
                    val volume = remember(spec, scanResult) { com.aistudio.aerofast.trmst.data.DrawingScan.volume(spec, scanResult) }
                    Spacer(modifier = Modifier.height(14.dp))
                    ProductionSummaryCard(spec = spec, result = scanResult)
                    Spacer(modifier = Modifier.height(10.dp))
                    DrawingVolumeCard(spec = spec, volume = volume, materialName = scanResult.material.name)

                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = customColors.bgElevated,
                        border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = "Read", tint = SuccessGreen, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = "SPECIFICATION READ FROM THE DRAWING", fontSize = 11.sp, fontWeight = FontWeight.Black, color = SuccessGreen)
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(text = spec.technicalSummary, fontSize = 11.sp, color = customColors.textMuted, modifier = Modifier.padding(bottom = 8.dp))

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                SpecItemRow("Type", spec.fastenerType.label)
                                SpecItemRow("Diameter", "${spec.nominalDiaMm} mm")
                                if (!spec.fastenerType.isUnthreaded && !spec.fastenerType.isWasher) SpecItemRow("Pitch", "${spec.pitchValue} mm")
                                if (!spec.fastenerType.isNut && !spec.fastenerType.isWasher) SpecItemRow("Length", "${spec.lengthMm} mm")
                                SpecItemRow("Material", spec.materialKey)
                                if (!spec.fastenerType.isWasher) SpecItemRow("Property Class", spec.propertyClass.label)
                                if (!spec.fastenerType.isUnthreaded && !spec.fastenerType.isWasher) SpecItemRow("Thread Class", spec.threadClass)
                                SpecItemRow("Coating", spec.coating.label)
                            }
                            if (spec.assumed.isNotEmpty()) {
                                Text(
                                    text = "Not on the drawing — default used: " + spec.assumed.joinToString("; "),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = customColors.accentPrimary,
                                    modifier = Modifier.padding(top = 6.dp)
                                )
                            }

                            if (com.aistudio.aerofast.trmst.data.BoltForging.applies(spec.fastenerType)) {
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
                                onClick = onAskCoPilot,
                                colors = ButtonDefaults.buttonColors(containerColor = customColors.accentPrimary),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().height(46.dp)
                            ) {
                                Text("Ask the Co-Pilot About This Drawing", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.height(8.dp))

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

                            AiProcessFlowCard(spec = spec, issues = scanResult.pokaYokeIssues)
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
fun AiProcessFlowCard(spec: ExtractedFastenerSpec, issues: List<com.aistudio.aerofast.trmst.data.PokaYokeIssue>) {
    val customColors = LocalCustomColors.current
    val mat = FastenerCatalog.MATERIALS[spec.materialKey] ?: FastenerCatalog.MATERIALS["10B21"]!!
    // Aluminium and copper alloys are headed cold; titanium, nickel and high-strength aerospace steels are headed warm
    val forgingMethod = when (com.aistudio.aerofast.trmst.data.PokaYokeEngine.materialFamily(mat)) {
        com.aistudio.aerofast.trmst.data.MaterialFamily.ALUMINUM, com.aistudio.aerofast.trmst.data.MaterialFamily.COPPER_ALLOY -> "Cold heading from annealed wire"
        com.aistudio.aerofast.trmst.data.MaterialFamily.TITANIUM, com.aistudio.aerofast.trmst.data.MaterialFamily.NICKEL_SUPERALLOY ->
            "Warm heading — temperature per the alloy's forging specification"
        else -> if (mat.isAero) "Warm heading — temperature per the alloy's forging specification" else "Multi-station cold heading"
    }

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
                    text = "PROCESS FLOW & POKA-YOKE CHECK OF THE DRAWING",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = customColors.textMain
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // The same release gate as the Sizing tab, run on the scanned part
            com.aistudio.aerofast.trmst.ui.components.PokaYokePanel(issues = issues)

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = customColors.bgElevated,
                border = BorderStroke(1.dp, customColors.border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(text = "1. Forging Method: $forgingMethod", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = customColors.accentSecondary)
                    Text(text = "2. Input Wire Sourcing: ${mat.rawMaterialSourcingUtsMpa} • Density: ${mat.density} g/cm³", fontSize = 9.5.sp, color = customColors.textMain, modifier = Modifier.padding(top = 2.dp))
                    Text(text = "3. Surface Lubrication: ${mat.lubricationCoating}", fontSize = 9.5.sp, color = customColors.textMain, modifier = Modifier.padding(top = 2.dp))
                    Text(text = "4. Heat Treatment: ${mat.heatTreatment}", fontSize = 9.5.sp, color = customColors.textMain, modifier = Modifier.padding(top = 2.dp))
                    Text(text = "5. Tooling Steels: ${mat.dieTooling}", fontSize = 9.5.sp, color = customColors.textMain, modifier = Modifier.padding(top = 2.dp))
                }
            }
        }
    }
}

/** The scanner's headline: total volume of the part as drawn, what it is made of, and how much of it the drawing really gave. */
@Composable
private fun DrawingVolumeCard(spec: ExtractedFastenerSpec, volume: com.aistudio.aerofast.trmst.data.DrawingVolume, materialName: String) {
    val context = LocalContext.current
    val customColors = LocalCustomColors.current
    fun n(v: Double, dp: Int = 2) = "%,.${dp}f".format(java.util.Locale.US, v)
    val statusText: String
    val statusColor: Color
    when {
        volume.totalMm3 <= 0.0 -> { statusText = "NOT CALCULATED — see the notes below"; statusColor = AlertRed }
        volume.isComplete -> { statusText = "COMPLETE — every dimension in this volume is from the drawing"; statusColor = SuccessGreen }
        volume.notOnDrawing.isNotEmpty() -> {
            statusText = "NOT COMPLETE — ${volume.notOnDrawing.size} dimension(s) are not on the drawing; the value used is listed below"
            statusColor = customColors.accentPrimary
        }
        else -> { statusText = "CHECK — read the notes below before using this volume"; statusColor = customColors.accentPrimary }
    }
    val summary = buildString {
        append("${spec.fastenerType.label} — total volume as per drawing (AeroFast Pro)\n")
        append("Total volume: ${n(volume.totalMm3, 0)} mm³ = ${n(volume.totalMm3 / 1000.0, 3)} cm³\n")
        append("Head volume : ${n(volume.headMm3, 0)} mm³${if (volume.headIsScaled) " (head share of the standard shape applied to the scanned total)" else ""}\n")
        append("Shank + thread volume: ${n(volume.shankMm3, 0)} mm³\n")
        append("Weight: ${n(volume.massG)} g ($materialName, ${volume.densityGPerCm3} g/cm³)\n")
        append("Cut-off: Ø${n(volume.wireDiaMm)} × ${n(volume.cutOffLengthMm)} mm\n")
        append("Basis: ${volume.basis.label}\n$statusText\n")
        volume.items.forEach { append("- ${it.feature}: ${it.detail} = ${n(it.volumeMm3, 0)} mm³ [${it.source.label}]\n") }
        volume.notOnDrawing.forEach { append("Not on drawing: $it\n") }
        volume.cautions.forEach { append("Note: $it\n") }
    }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = customColors.bgCard,
        border = BorderStroke(1.5.dp, statusColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("TOTAL VOLUME AS PER DRAWING", fontSize = 11.sp, fontWeight = FontWeight.Black, color = customColors.accentSecondary)
            Text(
                text = "${n(volume.totalMm3, 0)} mm³",
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                color = customColors.textMain
            )
            Text(
                text = "= ${n(volume.totalMm3 / 1000.0, 3)} cm³  •  ${n(volume.massG)} g  ($materialName, ${volume.densityGPerCm3} g/cm³)",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = customColors.textMain
            )
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = customColors.bgElevated,
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf(
                        Triple("HEAD", volume.headMm3, customColors.accentPrimary),
                        Triple("SHANK + THREAD", volume.shankMm3, customColors.textMain),
                        Triple("TOTAL", volume.totalMm3, customColors.accentSecondary)
                    ).forEach { (title, v, colour) ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(title, fontSize = 8.5.sp, fontWeight = FontWeight.Black, color = customColors.textMuted)
                            Text(
                                text = "${n(v, 0)} mm³",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                color = colour
                            )
                            Text("${n(v / 1000.0 * volume.densityGPerCm3, 2)} g", fontSize = 9.sp, color = customColors.textMuted)
                        }
                    }
                }
            }
            Text(
                text = if (volume.headIsScaled)
                    "Head volume is the head share of this type's standard shape applied to the scanned total; head + shank = total."
                else "Head volume is the material above the die face; head + shank = total.",
                fontSize = 9.sp, color = customColors.textMuted
            )
            Text(
                text = "Cut-off slug: Ø${n(volume.wireDiaMm)} × ${n(volume.cutOffLengthMm)} mm" +
                    if (volume.scrapMm3 > 0.5) "  (includes ${n(volume.scrapMm3, 0)} mm³ trim / pierce scrap)" else "",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = customColors.accentPrimary
            )
            Surface(shape = RoundedCornerShape(6.dp), color = statusColor.copy(alpha = 0.15f), modifier = Modifier.fillMaxWidth().padding(top = 2.dp)) {
                Text(statusText, fontSize = 10.sp, fontWeight = FontWeight.Black, color = statusColor, modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp))
            }

            volume.outline?.let {
                Spacer(modifier = Modifier.height(4.dp))
                PartOutlineCanvas(outline = it)
                Text("The part as understood from the drawing, to scale (long shanks broken). Compare it with the drawing.", fontSize = 9.sp, color = customColors.textMuted)
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text("VOLUME BREAKDOWN — ${volume.basis.label}", fontSize = 10.sp, fontWeight = FontWeight.Black, color = customColors.accentPrimary)
            volume.items.forEach { item ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(item.feature, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = customColors.textMain)
                        Text("${item.detail}  •  ${item.source.label}", fontSize = 9.sp, color = customColors.textMuted)
                    }
                    Text(
                        text = (if (item.volumeMm3 < 0) "− " else "") + "${n(kotlin.math.abs(item.volumeMm3), 0)} mm³",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = if (item.volumeMm3 < 0) AlertRed else customColors.textMain
                    )
                }
            }
            Row(modifier = Modifier.fillMaxWidth().padding(top = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("TOTAL", fontSize = 11.sp, fontWeight = FontWeight.Black, color = customColors.textMain)
                Text("${n(volume.totalMm3, 0)} mm³", fontSize = 11.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, color = customColors.textMain)
            }
            if (volume.basis == com.aistudio.aerofast.trmst.data.VolumeBasis.SOLIDS) {
                Text("Cross-check — standard shape of this fastener type: ${n(volume.modelVolumeMm3, 0)} mm³", fontSize = 9.5.sp, color = customColors.textMuted)
            }

            if (volume.notOnDrawing.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text("NOT ON THE DRAWING — VALUE USED INSTEAD (${volume.notOnDrawing.size})", fontSize = 10.sp, fontWeight = FontWeight.Black, color = customColors.accentPrimary)
                volume.notOnDrawing.forEach { Text("• $it", fontSize = 9.5.sp, color = customColors.textMain) }
            }
            if (volume.cautions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text("NOT IN THIS VOLUME / CHECK", fontSize = 10.sp, fontWeight = FontWeight.Black, color = AlertRed)
                volume.cautions.forEach { Text("• $it", fontSize = 9.5.sp, color = customColors.textMain) }
            }
            if (volume.fromDrawing.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text("FROM THE DRAWING (${volume.fromDrawing.size})", fontSize = 10.sp, fontWeight = FontWeight.Black, color = SuccessGreen)
                Text(volume.fromDrawing.joinToString("  •  "), fontSize = 9.5.sp, color = customColors.textMain)
            }
            Text(
                "Chamfers, fillet radii and the thread run-out are not counted (typically under 1 % of the volume).",
                fontSize = 9.sp, color = customColors.textMuted, modifier = Modifier.padding(top = 2.dp)
            )
            OutlinedButton(
                onClick = {
                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Volume", summary))
                    Toast.makeText(context, "Volume copied", Toast.LENGTH_SHORT).show()
                },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Copy Volume Breakdown", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
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
    val pitchMm = if (spec.unitSystem == com.aistudio.aerofast.trmst.data.UnitSystem.METRIC) spec.pitchValue
        else if (spec.pitchValue > 3.0) 25.4 / spec.pitchValue else spec.pitchValue
    val f = remember(spec) {
        val calc = com.aistudio.aerofast.trmst.data.CalculationEngine.calculate(
            fastenerType = spec.fastenerType, unitSystem = spec.unitSystem,
            standard = com.aistudio.aerofast.trmst.data.PokaYokeEngine.defaultStandard(spec.fastenerType, spec.unitSystem),
            nominalSizeIndex = spec.nominalSizeIndex, pitchValue = spec.pitchValue, lengthMm = spec.lengthMm,
            threadClass = spec.threadClass, material = material, coating = spec.coating, batchQty = 1,
            customNominalDiaMm = spec.nominalDiaMm, propertyClass = spec.propertyClass, drawing = spec.geometry()
        )
        val fl = if (spec.fastenerType == FastenerType.FLANGE_BOLT) com.aistudio.aerofast.trmst.data.BoltForging.resolveFlange(
            spec.nominalDiaMm, spec.flangeDiaMm, spec.headHeightMm, spec.headAcrossFlatsMm, spec.flangeThicknessMm) else null
        com.aistudio.aerofast.trmst.data.BoltForging.compute(
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

private val OCR_ROLES = listOf(
    "—", "Nominal d", "Length L", "Thread length b", "Shank Ø", "Across flats s", "Head Ø dk", "Head height k",
    "Flange Ø dc", "Flange thickness c", "Socket key s", "Socket depth t", "Square neck f", "Nut height m",
    "Washer outside Ø", "Washer bore Ø", "Washer thickness"
)

/** On-phone OCR result: the user assigns each number; type, material, class and coating come from the current Sizing selection. */
@Composable
private fun OcrAssignPanel(
    values: List<com.aistudio.aerofast.trmst.util.OcrValue>,
    current: com.aistudio.aerofast.trmst.data.FastenerCalculationResult,
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
                        propertyClass = com.aistudio.aerofast.trmst.data.ShopDefaults.propertyClass,
                        materialKey = com.aistudio.aerofast.trmst.data.ShopDefaults.material.key, threadClass = current.threadClass,
                        coating = com.aistudio.aerofast.trmst.data.ShopDefaults.coating,
                        technicalSummary = "Read on phone (OCR); ${assigned.size} value(s) assigned by you. Type from your Sizing selection; material, class and coating from your shop defaults.",
                        detectedDimensions = assigned.map { (i, role) -> DetectedDimension(role, "${values[i].value} mm", values[i].source.take(30)) },
                        isAiVisionExtracted = false,
                        modelUsed = "On-phone OCR + your assignment",
                        threadLengthMm = pick("Thread length b")?.value, shankDiaMm = pick("Shank Ø")?.value,
                        headAcrossFlatsMm = pick("Across flats s")?.value, headHeightMm = pick("Head height k")?.value,
                        flangeDiaMm = pick("Flange Ø dc")?.value, flangeThicknessMm = pick("Flange thickness c")?.value,
                        headDiaMm = pick("Head Ø dk")?.value, socketSizeMm = pick("Socket key s")?.value, socketDepthMm = pick("Socket depth t")?.value,
                        neckHeightMm = pick("Square neck f")?.value, nutHeightMm = pick("Nut height m")?.value,
                        washerOuterDiaMm = pick("Washer outside Ø")?.value, washerInnerDiaMm = pick("Washer bore Ø")?.value,
                        washerThicknessMm = pick("Washer thickness")?.value,
                        // Values you did not assign come from the Sizing selection, not from this drawing
                        assumed = buildList {
                            if (d == null) add("nominal diameter (not assigned: ${current.nominalDiaMm} mm from Sizing used)")
                            if (pick("Length L") == null) add("length (not assigned: ${current.lengthMm} mm from Sizing used)")
                        }
                    ))
                },
                enabled = roles.values.any { it != OCR_ROLES.first() },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Use these values", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
        }
    }
}

/** The plant's standing requirement, applied to anything the drawing does not state. */
@Composable
private fun ShopDefaultsCard() {
    val context = LocalContext.current
    val customColors = LocalCustomColors.current
    val d = com.aistudio.aerofast.trmst.data.ShopDefaults
    var editing by remember { mutableStateOf(false) }
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = customColors.bgElevated,
        border = BorderStroke(1.dp, customColors.accentSecondary.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "OUR REQUIREMENT (when the drawing does not say)", fontSize = 10.sp, fontWeight = FontWeight.Black, color = customColors.accentSecondary)
                    Text(
                        text = "${d.material.key} · ${d.propertyClass.label} · ${d.coating.label} · ${d.batchQty} pcs",
                        fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, color = customColors.textMain
                    )
                }
                TextButton(onClick = { editing = !editing }) {
                    Text(if (editing) "Done" else "Edit", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
            if (editing) {
                val materials = FastenerCatalog.MATERIALS.values.toList()
                SelectorDropdown(
                    label = "Material",
                    selectedText = "${d.material.key} — ${d.material.name}",
                    items = materials.map { "${it.key} — ${it.name}" },
                    onItemSelected = { d.materialKey = materials[it].key; d.save(context) },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    val classes = PropertyClass.values().toList()
                    SelectorDropdown(
                        label = "Property class",
                        selectedText = d.propertyClass.label,
                        items = classes.map { it.label },
                        onItemSelected = { d.propertyClass = classes[it]; d.save(context) },
                        modifier = Modifier.weight(1f)
                    )
                    val coatings = CoatingType.values().toList()
                    SelectorDropdown(
                        label = "Coating",
                        selectedText = d.coating.label,
                        items = coatings.map { it.label },
                        onItemSelected = { d.coating = coatings[it]; d.save(context) },
                        modifier = Modifier.weight(1f)
                    )
                }
                var batchText by remember { mutableStateOf(d.batchQty.toString()) }
                OutlinedTextField(
                    value = batchText,
                    onValueChange = { t ->
                        batchText = t
                        t.toIntOrNull()?.takeIf { it > 0 }?.let { d.batchQty = it; d.save(context) }
                    },
                    singleLine = true,
                    label = { Text("Batch quantity (pcs)", fontSize = 10.sp) },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "Material, class and coating apply to the next scan. A value printed on the drawing always wins. Batch quantity updates the report straight away.",
                    fontSize = 10.sp, color = customColors.textMuted
                )
            }
        }
    }
}

/** Headline production numbers for the scanned drawing, and the full report to share. */
@Composable
private fun ProductionSummaryCard(spec: ExtractedFastenerSpec, result: com.aistudio.aerofast.trmst.data.FastenerCalculationResult) {
    val context = LocalContext.current
    val customColors = LocalCustomColors.current
    val s = remember(result) { com.aistudio.aerofast.trmst.data.ProductionReport.summary(result) }
    fun n(v: Double, dp: Int = 2) = "%,.${dp}f".format(java.util.Locale.US, v)
    val critical = result.criticalIssues.size
    val mandatory = result.pokaYokeIssues.count { it.severity == com.aistudio.aerofast.trmst.data.PokaYokeSeverity.MANDATORY }
    val warnings = result.pokaYokeIssues.count { it.severity == com.aistudio.aerofast.trmst.data.PokaYokeSeverity.WARNING }
    val statusColor = when {
        critical > 0 -> AlertRed
        mandatory > 0 -> customColors.accentSecondary
        else -> SuccessGreen
    }
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = customColors.bgCard,
        border = BorderStroke(1.5.dp, customColors.accentPrimary),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(text = "PRODUCTION SUMMARY — ${result.threadLabel} x ${n(result.lengthMm, 1)} mm", fontSize = 11.sp, fontWeight = FontWeight.Black, color = customColors.accentPrimary)
            Text(
                text = "${result.material.key} · ${result.propertyClass?.label ?: "material spec"} · ${result.coating.label} · ${s.batchQty} pcs",
                fontSize = 10.sp, color = customColors.textMuted
            )
            val rowStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, color = customColors.textMain)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Wire Ø          : ${n(s.wireDiaMm)} mm", style = rowStyle)
            Text(text = "Cut-off length  : ${n(s.cutOffLengthMm)} mm", style = rowStyle.copy(color = customColors.accentPrimary))
            Text(text = "Finished weight : ${n(s.finishedWeightG, 3)} g", style = rowStyle)
            Text(text = "Slug weight     : ${n(s.slugWeightG, 3)} g (scrap ${n(s.scrapPct, 1)} %)", style = rowStyle)
            Text(text = "Per 1000 pcs    : ${n(s.finishedKgPer1000)} kg", style = rowStyle)
            Text(text = "Batch finished  : ${n(s.batchFinishedKg, 1)} kg", style = rowStyle)
            Text(text = "Raw material    : ${n(s.batchRawMaterialKg, 1)} kg", style = rowStyle)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = when {
                    critical > 0 -> "✗ Standard check: $critical critical item(s) — not releasable as drawn"
                    mandatory > 0 -> "⚠ Standard check: $mandatory mandatory control(s), $warnings warning(s)"
                    warnings > 0 -> "✓ Standard check passed with $warnings warning(s) to review"
                    else -> "✓ Standard check: all rules passed"
                },
                fontSize = 11.sp, fontWeight = FontWeight.Bold, color = statusColor
            )
            if (spec.assumed.isNotEmpty()) {
                Text(text = "Defaults used (not on the drawing): ${spec.assumed.joinToString("; ")}", fontSize = 10.sp, color = customColors.textMuted)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Button(
                onClick = { com.aistudio.aerofast.trmst.util.ProductionReportShare.share(context, result, spec) },
                colors = ButtonDefaults.buttonColors(containerColor = customColors.accentPrimary),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().height(46.dp)
            ) {
                Text("Share Full Production Report", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Text(
                text = "Cut-off, weights, wire options, anneal, heading stations, thread rolling, heat treatment, Zn-Ni baking, mechanical limits, standard check and recommendations.",
                fontSize = 9.5.sp, color = customColors.textMuted
            )
        }
    }
}
