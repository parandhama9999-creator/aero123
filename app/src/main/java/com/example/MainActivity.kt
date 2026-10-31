package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Fireplace
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.PrecisionManufacturing
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CalculationEngine
import com.example.data.CoatingType
import com.example.data.FastenerCatalog
import com.example.data.FastenerType
import com.example.data.ProductStandard
import com.example.data.PropertyClass
import com.example.data.SelectionFilter
import com.example.data.ThreadClassCatalog
import com.example.data.UnitSystem
import com.example.ui.components.PricingDialog
import com.example.ui.components.SettingsDialog
import com.example.ui.screens.AiCoPilotScreen
import com.example.ui.screens.AiScannerScreen
import com.example.ui.screens.FanucGCodeScreen
import com.example.ui.screens.FastenerSizingScreen
import com.example.ui.screens.ForgingMetallurgyScreen
import com.example.ui.screens.QuotationScreen
import com.example.ui.screens.EightDKnowledgeScreen
import com.example.ui.screens.PitchGaugeScreen
import com.example.ui.screens.JointTorqueScreen
import androidx.compose.material.icons.filled.Engineering
import com.example.ui.theme.AeroFastTheme
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.LocalCustomColors

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.example.data.AeroFastState
import com.example.data.ChatMessageData
import com.example.data.FirebaseStorageManager
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AeroFastApp()
        }
    }
}

enum class NavigationTab(val tabIndex: Int, val defaultTitle: String, val icon: ImageVector) {
    SIZING(1, "1. Sizing", Icons.Default.Tune),
    FORGING(2, "2. HT & Forging", Icons.Default.Fireplace),
    SCANNER(3, "3. AI Scanner", Icons.Default.CameraAlt),
    GCODE(4, "4. Fanuc G-Code", Icons.Default.Code),
    QUOTATION(5, "5. Quotation", Icons.AutoMirrored.Filled.ReceiptLong),
    COPILOT(6, "6. AI Co-Pilot", Icons.Default.SmartToy),
    EIGHT_D(7, "7. 8D NPD Engine", Icons.Default.Engineering),
    PITCH_GAUGE(8, "8. Pitch Gauge", Icons.Default.Straighten),
    JOINT_CALC(9, "9. Joint Torque", Icons.Default.Build);

    fun getTitle(lang: com.example.util.AppLanguage): String {
        return com.example.util.AppLocalization.getTabTitle(tabIndex, lang)
    }
}

@Composable
fun AeroFastApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var themeMode by remember { mutableStateOf(AppThemeMode.INDUSTRIAL_LIGHT) }
    var selectedFontOption by remember { mutableStateOf(com.example.ui.theme.AppFontOption.ROBOTO_MONO) }
    var currentTab by remember { mutableStateOf(NavigationTab.SIZING) }
    var currentLanguage by remember { mutableStateOf(com.example.util.AppLocalization.getSelectedLanguage(context)) }

    // 10 Poka-Yoke Fastener Parameters
    var selectedFastenerType by remember { mutableStateOf(FastenerType.HEX_BOLT) }
    var selectedUnitSystem by remember { mutableStateOf(UnitSystem.METRIC) }
    var selectedStandard by remember { mutableStateOf(ProductStandard.ISO_4014) }
    var selectedSizeIndex by remember { mutableIntStateOf(FastenerCatalog.defaultSizeIndex(UnitSystem.METRIC)) } // M10
    var selectedPitchValue by remember { mutableDoubleStateOf(1.50) }
    var selectedLength by remember { mutableDoubleStateOf(60.0) }
    var selectedPropClass by remember { mutableStateOf(PropertyClass.CLASS_10_9) }
    var selectedMaterialKey by remember { mutableStateOf("10B21") }
    var selectedThreadClass by remember { mutableStateOf("6g") }
    var selectedCoating by remember { mutableStateOf(CoatingType.ZN_NI) }
    var batchQty by remember { mutableIntStateOf(10000) }

    var appliedExtractedSpec by remember { mutableStateOf<com.example.data.ExtractedFastenerSpec?>(null) }

    // Quotation Parameters
    var currencyIsUsd by remember { mutableStateOf(false) }
    var cncHourRate by remember { mutableDoubleStateOf(750.0) }
    var htRatePerKg by remember { mutableDoubleStateOf(180.0) }
    var marginPct by remember { mutableDoubleStateOf(22.0) }

    // Dialogs & Tier
    var currentTier by remember { mutableStateOf("FREE") }
    var showPricingDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    // Chat History State
    var savedChatMessages by remember { mutableStateOf<List<ChatMessageData>>(emptyList()) }

    fun triggerFirebaseSync(
        scannedBlueprintTag: String? = null,
        chatMsgs: List<ChatMessageData>? = null
    ) {
        val msgs = chatMsgs ?: savedChatMessages
        val state = AeroFastState(
            fastenerType = selectedFastenerType.name,
            unitSystem = selectedUnitSystem.name,
            standard = selectedStandard.name,
            sizeIndex = selectedSizeIndex,
            sizeDiaMm = FastenerCatalog.diameterMm(selectedUnitSystem, selectedSizeIndex),
            pitchValue = selectedPitchValue,
            lengthMm = selectedLength,
            propClass = selectedPropClass.name,
            materialKey = selectedMaterialKey,
            threadClass = selectedThreadClass,
            coating = selectedCoating.name,
            batchQty = batchQty,
            currencyIsUsd = currencyIsUsd,
            cncHourRate = cncHourRate,
            htRatePerKg = htRatePerKg,
            marginPct = marginPct,
            fontOption = selectedFontOption.name,
            currentTier = currentTier,
            lastScannedBlueprint = scannedBlueprintTag ?: appliedExtractedSpec?.let { "Blueprint (${it.materialKey}, M${it.nominalDiaMm}mm)" },
            chatMessages = msgs
        )
        scope.launch {
            FirebaseStorageManager.syncAppData(context, state)
        }
    }

    // Startup restoration from Firebase Realtime Database with 2-second timeout fallback to local storage
    LaunchedEffect(Unit) {
        // Custom alloys first, so a saved part never falls back to a different material
        com.example.data.CustomMaterialStore.restoreInto(context)
        val loadedState = FirebaseStorageManager.fetchAppDataOnStartup(context)
        if (loadedState != null) {
            try { selectedFastenerType = FastenerType.valueOf(loadedState.fastenerType) } catch (_: Exception) {}
            try { selectedUnitSystem = UnitSystem.valueOf(loadedState.unitSystem) } catch (_: Exception) {}
            try { selectedStandard = ProductStandard.valueOf(loadedState.standard) } catch (_: Exception) {}
            selectedSizeIndex = loadedState.let { st ->
                val u = runCatching { UnitSystem.valueOf(st.unitSystem) }.getOrDefault(UnitSystem.METRIC)
                if (st.sizeDiaMm > 0.0) FastenerCatalog.sizeIndexFor(u, st.sizeDiaMm) else FastenerCatalog.migrateLegacySizeIndex(u, st.sizeIndex)
            }
            selectedPitchValue = loadedState.pitchValue
            selectedLength = loadedState.lengthMm
            try { selectedPropClass = PropertyClass.valueOf(loadedState.propClass) } catch (_: Exception) {}
            selectedMaterialKey = loadedState.materialKey
            selectedThreadClass = loadedState.threadClass
            try { selectedCoating = CoatingType.valueOf(loadedState.coating) } catch (_: Exception) {}
            batchQty = loadedState.batchQty
            currencyIsUsd = loadedState.currencyIsUsd
            cncHourRate = loadedState.cncHourRate
            htRatePerKg = loadedState.htRatePerKg
            marginPct = loadedState.marginPct
            try { selectedFontOption = com.example.ui.theme.AppFontOption.valueOf(loadedState.fontOption) } catch (_: Exception) {}
            currentTier = loadedState.currentTier
            savedChatMessages = loadedState.chatMessages
        }
    }

    val activeMaterial = FastenerCatalog.MATERIALS[selectedMaterialKey] ?: FastenerCatalog.MATERIALS["10B21"]!!
    // Never swap materials silently: tell the user when a saved alloy is unavailable
    LaunchedEffect(selectedMaterialKey, FastenerCatalog.MATERIALS.size) {
        if (FastenerCatalog.MATERIALS[selectedMaterialKey] == null) {
            Toast.makeText(context, "⚠️ Material '$selectedMaterialKey' not found — showing 10B21. Re-select or re-add the alloy.", Toast.LENGTH_LONG).show()
        }
    }

    // Keep every Sizing selector consistent with the ones above it, whichever path changed them
    // (dropdowns, alloy catalogue, drawing scan or a restored session)
    LaunchedEffect(
        selectedFastenerType, selectedUnitSystem, selectedStandard, selectedSizeIndex, selectedPropClass,
        selectedMaterialKey, selectedThreadClass, selectedCoating, appliedExtractedSpec, FastenerCatalog.MATERIALS.size
    ) {
        val current = SelectionFilter.Selection(
            type = selectedFastenerType,
            unit = selectedUnitSystem,
            standard = selectedStandard,
            dMm = appliedExtractedSpec?.nominalDiaMm ?: FastenerCatalog.diameterMm(selectedUnitSystem, selectedSizeIndex),
            propertyClass = selectedPropClass,
            materialKey = selectedMaterialKey,
            threadClass = selectedThreadClass,
            coating = selectedCoating
        )
        val fixed = SelectionFilter.normalize(current, FastenerCatalog.MATERIALS)
        if (fixed != current) {
            selectedStandard = fixed.standard
            selectedPropClass = fixed.propertyClass
            selectedMaterialKey = fixed.materialKey
            selectedThreadClass = fixed.threadClass
            selectedCoating = fixed.coating
            triggerFirebaseSync()
        }
    }

    // Active Fastener Calculation
    val currentCalculation = remember(
        selectedFastenerType,
        selectedUnitSystem,
        selectedStandard,
        selectedSizeIndex,
        selectedPitchValue,
        selectedLength,
        selectedThreadClass,
        activeMaterial,
        selectedCoating,
        batchQty,
        currencyIsUsd,
        cncHourRate,
        htRatePerKg,
        marginPct,
        selectedPropClass,
        appliedExtractedSpec
    ) {
        CalculationEngine.calculate(
            fastenerType = selectedFastenerType,
            unitSystem = selectedUnitSystem,
            standard = selectedStandard,
            nominalSizeIndex = selectedSizeIndex,
            pitchValue = selectedPitchValue,
            lengthMm = selectedLength,
            threadClass = selectedThreadClass,
            material = activeMaterial,
            coating = selectedCoating,
            batchQty = batchQty,
            currencyIsUsd = currencyIsUsd,
            cncHourRateInr = cncHourRate,
            htRatePerKgInr = htRatePerKg,
            marginPct = marginPct,
            customNominalDiaMm = appliedExtractedSpec?.nominalDiaMm,
            extractedRadius = appliedExtractedSpec?.radius,
            propertyClass = selectedPropClass
        )
    }

    fun reloadStateFromFirebase() {
        scope.launch {
            val loadedState = FirebaseStorageManager.fetchAppDataOnStartup(context)
            if (loadedState != null) {
                try { selectedFastenerType = FastenerType.valueOf(loadedState.fastenerType) } catch (_: Exception) {}
                try { selectedUnitSystem = UnitSystem.valueOf(loadedState.unitSystem) } catch (_: Exception) {}
                try { selectedStandard = ProductStandard.valueOf(loadedState.standard) } catch (_: Exception) {}
                selectedSizeIndex = loadedState.let { st ->
                val u = runCatching { UnitSystem.valueOf(st.unitSystem) }.getOrDefault(UnitSystem.METRIC)
                if (st.sizeDiaMm > 0.0) FastenerCatalog.sizeIndexFor(u, st.sizeDiaMm) else FastenerCatalog.migrateLegacySizeIndex(u, st.sizeIndex)
            }
                selectedPitchValue = loadedState.pitchValue
                selectedLength = loadedState.lengthMm
                try { selectedPropClass = PropertyClass.valueOf(loadedState.propClass) } catch (_: Exception) {}
                selectedMaterialKey = loadedState.materialKey
                selectedThreadClass = loadedState.threadClass
                try { selectedCoating = CoatingType.valueOf(loadedState.coating) } catch (_: Exception) {}
                batchQty = loadedState.batchQty
                currencyIsUsd = loadedState.currencyIsUsd
                cncHourRate = loadedState.cncHourRate
                htRatePerKg = loadedState.htRatePerKg
                marginPct = loadedState.marginPct
                try { selectedFontOption = com.example.ui.theme.AppFontOption.valueOf(loadedState.fontOption) } catch (_: Exception) {}
                currentTier = loadedState.currentTier
                savedChatMessages = loadedState.chatMessages
                Toast.makeText(context, "Refreshed & synced with Cloud RTDB", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "State refreshed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    androidx.compose.runtime.CompositionLocalProvider(com.example.util.LocalAppLanguage provides currentLanguage) {
    AeroFastTheme(themeMode = themeMode, fontOption = selectedFontOption) {
        val customColors = LocalCustomColors.current

        var globalZoomScale by remember { mutableFloatStateOf(1.0f) }
        var globalPanX by remember { mutableFloatStateOf(0f) }
        var globalPanY by remember { mutableFloatStateOf(0f) }

        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            do {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                val pressedPointers = event.changes.filter { it.pressed }

                                if (pressedPointers.size >= 2) {
                                    val zoomChange = event.calculateZoom()
                                    val panChange = event.calculatePan()

                                    if (zoomChange != 1.0f || panChange != Offset.Zero) {
                                        val newScale = (globalZoomScale * zoomChange).coerceIn(1.0f, 3.5f)
                                        globalZoomScale = newScale

                                        if (newScale > 1.0f) {
                                            globalPanX += panChange.x
                                            globalPanY += panChange.y
                                        } else {
                                            globalPanX = 0f
                                            globalPanY = 0f
                                        }

                                        event.changes.forEach { it.consume() }
                                    }
                                } else if (globalZoomScale > 1.05f && pressedPointers.size == 1) {
                                    val panChange = event.calculatePan()
                                    if (panChange != Offset.Zero) {
                                        globalPanX += panChange.x
                                        globalPanY += panChange.y
                                        event.changes.forEach { it.consume() }
                                    }
                                }
                            } while (event.changes.any { it.pressed })
                        }
                    }
                    .graphicsLayer(
                        scaleX = globalZoomScale,
                        scaleY = globalZoomScale,
                        translationX = globalPanX,
                        translationY = globalPanY
                    )
            ) {
            Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            containerColor = customColors.bgMain,
            topBar = {
                AeroFastTopBar(
                    themeMode = themeMode,
                    onThemeChanged = {
                        themeMode = it
                        triggerFirebaseSync()
                    },
                    fontOption = selectedFontOption,
                    onFontChanged = {
                        selectedFontOption = it
                        triggerFirebaseSync()
                    },
                    currencyIsUsd = currencyIsUsd,
                    onCurrencyToggled = {
                        currencyIsUsd = it
                        triggerFirebaseSync()
                    },
                    currentTier = currentTier,
                    onOpenPricing = { showPricingDialog = true },
                    onOpenSettings = { showSettingsDialog = true },
                    onRefresh = { reloadStateFromFirebase() },
                    currentLanguage = currentLanguage
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Navigation Tabs Bar
                AeroFastNavBar(
                    selectedTab = currentTab,
                    onTabSelected = { currentTab = it },
                    currentLanguage = currentLanguage
                )

                // Tab Content Pane
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    when (currentTab) {
                        NavigationTab.SIZING -> {
                            FastenerSizingScreen(
                                result = currentCalculation,
                                selectedFastenerType = selectedFastenerType,
                                onFastenerTypeSelected = { newType ->
                                    selectedFastenerType = newType
                                    appliedExtractedSpec = null
                                    selectedStandard = com.example.data.PokaYokeEngine.defaultStandard(newType, selectedUnitSystem)
                                    val validClasses = ThreadClassCatalog.getOptions(selectedUnitSystem, newType.isNut)
                                    if (selectedThreadClass !in validClasses) {
                                        selectedThreadClass = validClasses.first()
                                    }
                                    triggerFirebaseSync()
                                },
                                selectedUnitSystem = selectedUnitSystem,
                                onUnitSystemSelected = { newUnit ->
                                    selectedUnitSystem = newUnit
                                    selectedSizeIndex = FastenerCatalog.defaultSizeIndex(newUnit)
                                    selectedPitchValue = if (newUnit == UnitSystem.METRIC) 1.50 else 16.0
                                    selectedLength = if (newUnit == UnitSystem.METRIC) 60.0 else 50.8
                                    val validClasses = ThreadClassCatalog.getOptions(newUnit, selectedFastenerType.isNut)
                                    selectedThreadClass = validClasses.first()
                                    // Metric ↔ inch also changes the governing standard (ISO/DIN ↔ ASME/ASTM)
                                    selectedStandard = com.example.data.PokaYokeEngine.defaultStandard(selectedFastenerType, newUnit)
                                    appliedExtractedSpec = null
                                    triggerFirebaseSync()
                                },
                                selectedStandard = selectedStandard,
                                onStandardSelected = {
                                    selectedStandard = it
                                    appliedExtractedSpec = null
                                    triggerFirebaseSync()
                                },
                                selectedSizeIndex = selectedSizeIndex,
                                onSizeIndexSelected = { idx ->
                                    selectedSizeIndex = idx
                                    if (selectedUnitSystem == UnitSystem.METRIC) {
                                        val m = FastenerCatalog.METRIC_SIZES.getOrElse(idx) { FastenerCatalog.METRIC_SIZES[FastenerCatalog.defaultSizeIndex(UnitSystem.METRIC)] }
                                        selectedPitchValue = m.pitches.first()
                                    } else {
                                        val inc = FastenerCatalog.INCH_SIZES.getOrElse(idx) { FastenerCatalog.INCH_SIZES[FastenerCatalog.defaultSizeIndex(UnitSystem.INCH)] }
                                        selectedPitchValue = inc.tpis.first().toDouble()
                                    }
                                    appliedExtractedSpec = null
                                    triggerFirebaseSync()
                                },
                                selectedPitchValue = selectedPitchValue,
                                onPitchSelected = {
                                    selectedPitchValue = it
                                    appliedExtractedSpec = null
                                    triggerFirebaseSync()
                                },
                                selectedLength = selectedLength,
                                onLengthSelected = {
                                    selectedLength = it
                                    triggerFirebaseSync()
                                },
                                selectedPropClass = selectedPropClass,
                                onPropClassSelected = {
                                    selectedPropClass = it
                                    appliedExtractedSpec = null
                                    triggerFirebaseSync()
                                },
                                selectedMaterialKey = selectedMaterialKey,
                                onMaterialSelected = {
                                    selectedMaterialKey = it
                                    appliedExtractedSpec = null
                                    triggerFirebaseSync()
                                },
                                selectedThreadClass = selectedThreadClass,
                                onThreadClassSelected = {
                                    selectedThreadClass = it
                                    appliedExtractedSpec = null
                                    triggerFirebaseSync()
                                },
                                selectedCoating = selectedCoating,
                                onCoatingSelected = {
                                    selectedCoating = it
                                    triggerFirebaseSync()
                                },
                                batchQty = batchQty,
                                onBatchQtyChanged = {
                                    batchQty = it
                                    triggerFirebaseSync()
                                },
                                appliedExtractedSpec = appliedExtractedSpec,
                                onClearCustomScan = {
                                    appliedExtractedSpec = null
                                    triggerFirebaseSync()
                                }
                            )
                        }

                        NavigationTab.FORGING -> {
                            ForgingMetallurgyScreen(result = currentCalculation)
                        }

                        NavigationTab.SCANNER -> {
                            AiScannerScreen(
                                currentResult = currentCalculation,
                                onApplyExtractedSpec = { spec ->
                                    appliedExtractedSpec = spec
                                    selectedFastenerType = spec.fastenerType
                                    selectedUnitSystem = spec.unitSystem
                                    selectedSizeIndex = spec.nominalSizeIndex
                                    selectedPitchValue = spec.pitchValue
                                    selectedLength = spec.lengthMm
                                    selectedPropClass = spec.propertyClass
                                    selectedMaterialKey = spec.materialKey
                                    // Keep a scanned thread class only if it is valid for this unit and internal/external thread
                                    val scanClasses = ThreadClassCatalog.getOptions(spec.unitSystem, spec.fastenerType.isNut)
                                    selectedThreadClass = scanClasses.firstOrNull { it.equals(spec.threadClass, ignoreCase = false) }
                                        ?: scanClasses.firstOrNull { it.equals(spec.threadClass, ignoreCase = true) }
                                        ?: scanClasses.first()
                                    selectedCoating = spec.coating
                                    selectedStandard = com.example.data.PokaYokeEngine.defaultStandard(spec.fastenerType, spec.unitSystem)
                                    currentTab = NavigationTab.SIZING
                                    triggerFirebaseSync(scannedBlueprintTag = "${spec.fastenerType.label} (${spec.materialKey}, M${spec.nominalDiaMm}mm)")
                                }
                            )
                        }

                        NavigationTab.GCODE -> {
                            FanucGCodeScreen(result = currentCalculation)
                        }

                        NavigationTab.QUOTATION -> {
                            QuotationScreen(
                                result = currentCalculation,
                                currencyIsUsd = currencyIsUsd,
                                onCurrencyToggled = {
                                    currencyIsUsd = it
                                    triggerFirebaseSync()
                                },
                                cncHourRate = cncHourRate,
                                onCncHourRateChanged = {
                                    cncHourRate = it
                                    triggerFirebaseSync()
                                },
                                htRatePerKg = htRatePerKg,
                                onHtRateChanged = {
                                    htRatePerKg = it
                                    triggerFirebaseSync()
                                },
                                marginPct = marginPct,
                                onMarginChanged = {
                                    marginPct = it
                                    triggerFirebaseSync()
                                }
                            )
                        }

                        NavigationTab.COPILOT -> {
                            AiCoPilotScreen(
                                currentResult = currentCalculation,
                                extractedSpec = appliedExtractedSpec,
                                initialMessages = savedChatMessages,
                                onChatMessageSent = { updatedMsgs ->
                                    savedChatMessages = updatedMsgs
                                    triggerFirebaseSync(chatMsgs = updatedMsgs)
                                }
                            )
                        }

                        NavigationTab.EIGHT_D -> {
                            EightDKnowledgeScreen(currentResult = currentCalculation)
                        }

                        NavigationTab.PITCH_GAUGE -> {
                            PitchGaugeScreen(result = currentCalculation)
                        }

                        NavigationTab.JOINT_CALC -> {
                            JointTorqueScreen(result = currentCalculation)
                        }
                    }
                }
            }
        }
    }

    // Floating Zoom Controller HUD Overlay on top of every page
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = customColors.bgCard.copy(alpha = 0.92f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (globalZoomScale > 1.05f) customColors.accentPrimary else customColors.border
        ),
        shadowElevation = 6.dp,
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .navigationBarsPadding()
            .padding(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clickable {
                        Toast.makeText(
                            context,
                            "Pinch with 2 fingers to zoom on any page",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    .padding(horizontal = 4.dp)
            ) {
                Icon(
                    Icons.Default.Straighten,
                    contentDescription = "Zoom",
                    tint = if (globalZoomScale > 1.05f) customColors.accentPrimary else customColors.textMuted,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = "${(globalZoomScale * 100).toInt()}%",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = if (globalZoomScale > 1.05f) customColors.accentPrimary else customColors.textMain
                )
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = customColors.bgElevated,
                modifier = Modifier
                    .size(24.dp)
                    .clickable {
                        globalZoomScale = (globalZoomScale - 0.25f).coerceIn(1.0f, 3.5f)
                        if (globalZoomScale <= 1.0f) {
                            globalZoomScale = 1.0f
                            globalPanX = 0f
                            globalPanY = 0f
                        }
                    }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("-", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = customColors.textMain)
                }
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = customColors.bgElevated,
                modifier = Modifier
                    .size(24.dp)
                    .clickable {
                        globalZoomScale = (globalZoomScale + 0.25f).coerceIn(1.0f, 3.5f)
                    }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("+", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = customColors.textMain)
                }
            }

            if (globalZoomScale > 1.05f) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = customColors.accentPrimary,
                    modifier = Modifier
                        .clickable {
                            globalZoomScale = 1.0f
                            globalPanX = 0f
                            globalPanY = 0f
                        }
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "Reset",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

if (showPricingDialog) {
    PricingDialog(
        currentTier = currentTier,
        onTierSelected = { currentTier = it },
        onDismiss = { showPricingDialog = false }
    )
}

if (showSettingsDialog) {
    SettingsDialog(
        onDismiss = { showSettingsDialog = false },
        onLanguageChanged = { newLang -> currentLanguage = newLang }
    )
}
}
}
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AeroFastTopBar(
    themeMode: AppThemeMode,
    onThemeChanged: (AppThemeMode) -> Unit,
    fontOption: com.example.ui.theme.AppFontOption = com.example.ui.theme.AppFontOption.ROBOTO_MONO,
    onFontChanged: (com.example.ui.theme.AppFontOption) -> Unit = {},
    currencyIsUsd: Boolean,
    onCurrencyToggled: (Boolean) -> Unit,
    currentTier: String,
    onOpenPricing: () -> Unit,
    onOpenSettings: () -> Unit,
    onRefresh: () -> Unit = {},
    currentLanguage: com.example.util.AppLanguage = com.example.util.LocalAppLanguage.current
) {
    val context = LocalContext.current
    val customColors = LocalCustomColors.current
    var themeMenuExpanded by remember { mutableStateOf(false) }

    Surface(
        color = customColors.bgCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, customColors.border),
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
    ) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.Center
            ) {
                // Title and Logo
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 1.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = customColors.accentPrimary,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.PrecisionManufacturing,
                                contentDescription = "Logo",
                                tint = Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(5.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "AEROFAST PRO",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Black,
                                color = customColors.accentPrimary
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = customColors.accentSecondary
                            ) {
                                Text(
                                    text = "M0.25–M64",
                                    fontSize = 7.5.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.Black,
                                    modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Text(
                            text = com.example.util.AppLocalization.t("app_subtitle", currentLanguage),
                            fontSize = 8.sp,
                            color = customColors.textMuted
                        )
                    }
                }

                // Controls: wrap cleanly with compact padding (flex-wrap gap-1.5)
                FlowRow(
                    verticalArrangement = Arrangement.Center,
                    horizontalArrangement = Arrangement.spacedBy(3.dp, Alignment.End),
                    modifier = Modifier.padding(vertical = 1.dp)
                ) {
                    // Currency Toggle (₹ / $)
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = customColors.bgElevated,
                        border = androidx.compose.foundation.BorderStroke(1.dp, customColors.border)
                    ) {
                        Row(modifier = Modifier.padding(1.dp)) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (!currencyIsUsd) customColors.accentPrimary else Color.Transparent,
                                modifier = Modifier.clickable { onCurrencyToggled(false) }
                            ) {
                                Text(
                                    text = "₹",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (!currencyIsUsd) Color.White else customColors.textMuted,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (currencyIsUsd) customColors.accentPrimary else Color.Transparent,
                                modifier = Modifier.clickable { onCurrencyToggled(true) }
                            ) {
                                Text(
                                    text = "$",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (currencyIsUsd) Color.White else customColors.textMuted,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    // Theme selector dropdown
                    Box {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = customColors.bgElevated,
                            border = androidx.compose.foundation.BorderStroke(1.dp, customColors.border),
                            modifier = Modifier.clickable { themeMenuExpanded = true }
                        ) {
                            Text(
                                text = "${themeMode.icon} ${com.example.util.AppLocalization.t("theme", currentLanguage)}",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = customColors.textMain,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 3.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = themeMenuExpanded,
                            onDismissRequest = { themeMenuExpanded = false },
                            modifier = Modifier.background(customColors.bgCard)
                        ) {
                            AppThemeMode.values().forEach { mode ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "${mode.icon} ${mode.displayName}",
                                            fontSize = 11.sp,
                                            fontWeight = if (mode == themeMode) FontWeight.Black else FontWeight.Normal,
                                            color = if (mode == themeMode) customColors.accentPrimary else customColors.textMain
                                        )
                                    },
                                    onClick = {
                                        onThemeChanged(mode)
                                        themeMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Font Family selector dropdown with search
                    Box {
                        var fontMenuExpanded by remember { mutableStateOf(false) }
                        var fontSearchQuery by remember { mutableStateOf("") }

                        val filteredFonts = remember(fontSearchQuery) {
                            val all = com.example.ui.theme.AppFontOption.values().toList()
                            if (fontSearchQuery.isBlank()) all
                            else all.filter { it.displayName.contains(fontSearchQuery, ignoreCase = true) }
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = customColors.bgElevated,
                            border = androidx.compose.foundation.BorderStroke(1.dp, customColors.border),
                            modifier = Modifier.clickable {
                                fontSearchQuery = ""
                                fontMenuExpanded = true
                            }
                        ) {
                            Text(
                                text = "${fontOption.icon} ${com.example.util.AppLocalization.t("font", currentLanguage)}",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = customColors.textMain,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 3.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = fontMenuExpanded,
                            onDismissRequest = { fontMenuExpanded = false },
                            modifier = Modifier
                                .background(customColors.bgCard)
                                .heightIn(max = 380.dp)
                        ) {
                            androidx.compose.material3.OutlinedTextField(
                                value = fontSearchQuery,
                                onValueChange = { fontSearchQuery = it },
                                placeholder = { Text("Search fonts...", fontSize = 11.sp, color = customColors.textMuted) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                singleLine = true,
                                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = customColors.bgElevated,
                                    unfocusedContainerColor = customColors.bgElevated,
                                    focusedBorderColor = customColors.accentPrimary,
                                    unfocusedBorderColor = customColors.border,
                                    focusedTextColor = customColors.textMain,
                                    unfocusedTextColor = customColors.textMain
                                )
                            )

                            if (filteredFonts.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("No matching fonts", fontSize = 11.sp, color = customColors.textMuted) },
                                    onClick = { }
                                )
                            } else {
                                filteredFonts.forEach { font ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = "${font.icon} ${font.displayName}",
                                                fontSize = 11.sp,
                                                fontWeight = if (font == fontOption) FontWeight.Black else FontWeight.Normal,
                                                color = if (font == fontOption) customColors.accentPrimary else customColors.textMain
                                            )
                                        },
                                        onClick = {
                                            onFontChanged(font)
                                            fontMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // View Orientation Toggle (Portrait vs Landscape Layout Mode)
                    var isLandscapeMode by remember { mutableStateOf(false) }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isLandscapeMode) customColors.accentPrimary.copy(alpha = 0.2f) else customColors.bgElevated,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isLandscapeMode) customColors.accentPrimary else customColors.border),
                        modifier = Modifier.clickable {
                            isLandscapeMode = !isLandscapeMode
                            (context as? android.app.Activity)?.requestedOrientation =
                                if (isLandscapeMode) android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                                else android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                            Toast.makeText(
                                context,
                                if (isLandscapeMode) "Landscape Mode Layout Active 🔄" else "Portrait Mode Layout Active 📱",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    ) {
                        Text(
                            text = if (isLandscapeMode) "🔄 Land" else "📱 Port",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isLandscapeMode) customColors.accentPrimary else customColors.textMain,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 3.dp)
                        )
                    }

                    // Tier button
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = customColors.accentSecondary.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, customColors.accentSecondary),
                        modifier = Modifier.clickable { onOpenPricing() }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                Icons.Default.WorkspacePremium,
                                contentDescription = "Tier",
                                tint = customColors.accentSecondary,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = currentTier,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Black,
                                color = customColors.accentSecondary
                            )
                        }
                    }

                    // Refresh / Sync Button with Toast
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = customColors.bgElevated,
                        border = androidx.compose.foundation.BorderStroke(1.dp, customColors.accentPrimary.copy(alpha = 0.6f)),
                        modifier = Modifier.clickable {
                            onRefresh()
                            Toast.makeText(context, "🔄 AeroFast catalog & state refreshed!", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Refresh & Sync",
                                tint = customColors.accentPrimary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = com.example.util.AppLocalization.t("sync", currentLanguage),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = customColors.accentPrimary
                            )
                        }
                    }

                    // API Key Settings Button
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = customColors.bgElevated,
                        border = androidx.compose.foundation.BorderStroke(1.dp, customColors.accentSecondary.copy(alpha = 0.6f)),
                        modifier = Modifier.clickable { onOpenSettings() }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                Icons.Default.Key,
                                contentDescription = "API Key",
                                tint = customColors.accentSecondary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = com.example.util.AppLocalization.t("key", currentLanguage),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = customColors.accentSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AeroFastNavBar(
    selectedTab: NavigationTab,
    onTabSelected: (NavigationTab) -> Unit,
    currentLanguage: com.example.util.AppLanguage = com.example.util.LocalAppLanguage.current
) {
    val customColors = LocalCustomColors.current
    val scrollState = rememberScrollState()

    Surface(
        color = customColors.bgCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, customColors.border)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            NavigationTab.values().forEach { tab ->
                val isSelected = tab == selectedTab
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) customColors.accentPrimary else customColors.bgElevated,
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) customColors.accentPrimary else customColors.border),
                    modifier = Modifier.clickable { onTabSelected(tab) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            tab.icon,
                            contentDescription = tab.getTitle(currentLanguage),
                            tint = if (isSelected) Color.White else customColors.textMuted,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = tab.getTitle(currentLanguage),
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                            color = if (isSelected) Color.White else customColors.textMain
                        )
                    }
                }
            }
        }
    }
}
