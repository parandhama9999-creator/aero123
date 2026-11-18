package com.aistudio.aerofast.trmst

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
import androidx.compose.material.icons.filled.Fireplace
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.PrecisionManufacturing
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Straighten
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
import com.aistudio.aerofast.trmst.data.CalculationEngine
import com.aistudio.aerofast.trmst.data.CoatingType
import com.aistudio.aerofast.trmst.data.FastenerCatalog
import com.aistudio.aerofast.trmst.data.FastenerType
import com.aistudio.aerofast.trmst.data.ProductStandard
import com.aistudio.aerofast.trmst.data.PropertyClass
import com.aistudio.aerofast.trmst.data.SelectionFilter
import com.aistudio.aerofast.trmst.data.ThreadClassCatalog
import com.aistudio.aerofast.trmst.data.UnitSystem
import com.aistudio.aerofast.trmst.ui.components.PricingDialog
import com.aistudio.aerofast.trmst.ui.components.SettingsDialog
import com.aistudio.aerofast.trmst.ui.screens.AiCoPilotScreen
import com.aistudio.aerofast.trmst.ui.screens.AiScannerScreen
import com.aistudio.aerofast.trmst.ui.screens.FastenerSizingScreen
import com.aistudio.aerofast.trmst.ui.screens.ForgingMetallurgyScreen
import com.aistudio.aerofast.trmst.ui.screens.EightDKnowledgeScreen
import androidx.compose.material.icons.filled.Engineering
import com.aistudio.aerofast.trmst.ui.theme.AeroFastTheme
import com.aistudio.aerofast.trmst.ui.theme.AppThemeMode
import com.aistudio.aerofast.trmst.ui.theme.LocalCustomColors

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.aistudio.aerofast.trmst.data.AeroFastState
import com.aistudio.aerofast.trmst.data.ChatMessageData
import com.aistudio.aerofast.trmst.data.AppStateStore
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // The app composes behind the intro, so it is ready the moment the intro ends
            var showIntro by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(true) }
            Box(modifier = Modifier.fillMaxSize()) {
                AeroFastApp()
                if (showIntro) com.aistudio.aerofast.trmst.ui.components.ForgeIntro(onFinished = { showIntro = false })
            }
            androidx.compose.runtime.LaunchedEffect(showIntro) {
                if (!showIntro) com.aistudio.aerofast.trmst.ui.components.AppIntro.finished = true
            }
        }
    }
}

enum class NavigationTab(val tabIndex: Int, val defaultTitle: String, val icon: ImageVector) {
    SIZING(1, "1. Sizing", Icons.Default.Tune),
    FORGING(2, "2. HT & Forging", Icons.Default.Fireplace),
    SCANNER(3, "3. AI Scanner", Icons.Default.CameraAlt),
    COPILOT(4, "4. AI Co-Pilot", Icons.Default.SmartToy),
    EIGHT_D(5, "5. 8D NPD Engine", Icons.Default.Engineering);

    fun getTitle(lang: com.aistudio.aerofast.trmst.util.AppLanguage): String {
        return com.aistudio.aerofast.trmst.util.AppLocalization.getTabTitle(tabIndex, lang)
    }
}

/** A two-finger gesture is a zoom only once the scale has changed by more than this. */
private const val PINCH_SLOP = 0.02f

@Composable
fun AeroFastApp() {
    val context = LocalContext.current
    // One-time engineering disclaimer (Play policy and liability): results are a reference, not a released design
    val disclaimerPrefs = remember { context.getSharedPreferences("aerofast_prefs", android.content.Context.MODE_PRIVATE) }
    var showDisclaimer by remember { mutableStateOf(!disclaimerPrefs.getBoolean("disclaimer_ok_v1", false)) }
    if (showDisclaimer && com.aistudio.aerofast.trmst.ui.components.AppIntro.finished) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = {},
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    disclaimerPrefs.edit().putBoolean("disclaimer_ok_v1", true).apply()
                    showDisclaimer = false
                }) { Text("I understand") }
            },
            title = { Text("Engineering reference only") },
            text = {
                Text(
                    "AeroFast Pro calculates fastener dimensions, processes and checks from published standards and " +
                        "material data. Results are for reference: verify every value against the current ISO / ASME / SAE / " +
                        "AMS standard, the customer drawing and your process qualification before production. " +
                        "The author accepts no liability for parts made from these results."
                )
            }
        )
    }
    val scope = rememberCoroutineScope()

    var themeMode by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(AppThemeMode.INDUSTRIAL_LIGHT) }
    var selectedFontOption by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(com.aistudio.aerofast.trmst.ui.theme.AppFontOption.ROBOTO_MONO) }
    var currentTab by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(NavigationTab.SIZING) }
    var currentLanguage by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(com.aistudio.aerofast.trmst.util.AppLocalization.getSelectedLanguage(context)) }

    // 10 Poka-Yoke Fastener Parameters
    var selectedFastenerType by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(FastenerType.HEX_BOLT) }
    var selectedUnitSystem by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(UnitSystem.METRIC) }
    var selectedStandard by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(ProductStandard.ISO_4014) }
    var selectedSizeIndex by androidx.compose.runtime.saveable.rememberSaveable { mutableIntStateOf(FastenerCatalog.defaultSizeIndex(UnitSystem.METRIC)) } // M10
    var selectedPitchValue by androidx.compose.runtime.saveable.rememberSaveable { mutableDoubleStateOf(1.50) }
    var selectedLength by androidx.compose.runtime.saveable.rememberSaveable { mutableDoubleStateOf(60.0) }
    // Customer thread length b in mm; 0 = take it from the applied drawing or the standard rule
    var customThreadLengthMm by androidx.compose.runtime.saveable.rememberSaveable { mutableDoubleStateOf(0.0) }
    var selectedPropClass by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(PropertyClass.CLASS_10_9) }
    var selectedMaterialKey by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("10B21") }
    var selectedThreadClass by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("6g") }
    var selectedCoating by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(CoatingType.ZN_NI) }
    var batchQty by androidx.compose.runtime.saveable.rememberSaveable { mutableIntStateOf(10000) }

    var appliedExtractedSpec by remember { mutableStateOf<com.aistudio.aerofast.trmst.data.ExtractedFastenerSpec?>(null) }
    // null = let the part decide between a 1-die header and a progressive former
    // The last scanned drawing: kept across tabs, and the Co-Pilot's primary input
    var scannedSpec by remember { mutableStateOf<com.aistudio.aerofast.trmst.data.ExtractedFastenerSpec?>(null) }
    val scanCalculation = remember(scannedSpec, FastenerCatalog.MATERIALS.size) {
        scannedSpec?.let { runCatching { com.aistudio.aerofast.trmst.data.DrawingScan.calculate(it) }.getOrNull() }
    }

    // Quotation Parameters
    var currencyIsUsd by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    var cncHourRate by androidx.compose.runtime.saveable.rememberSaveable { mutableDoubleStateOf(750.0) }
    var htRatePerKg by androidx.compose.runtime.saveable.rememberSaveable { mutableDoubleStateOf(180.0) }
    var marginPct by androidx.compose.runtime.saveable.rememberSaveable { mutableDoubleStateOf(22.0) }

    // Dialogs & Tier
    var currentTier by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("FREE") }
    var showPricingDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    // Chat History State
    var savedChatMessages by remember { mutableStateOf<List<ChatMessageData>>(emptyList()) }

    fun saveAppState(
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
            customThreadLengthMm = customThreadLengthMm,
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
            AppStateStore.save(context, state)
        }
    }

    // Startup restoration from the phone's own storage
    LaunchedEffect(Unit) {
        // Custom alloys first, so a saved part never falls back to a different material
        com.aistudio.aerofast.trmst.data.CustomMaterialStore.restoreInto(context)
        val loadedState = AppStateStore.load(context)
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
            customThreadLengthMm = loadedState.customThreadLengthMm
            try { selectedPropClass = PropertyClass.valueOf(loadedState.propClass) } catch (_: Exception) {}
            selectedMaterialKey = loadedState.materialKey
            selectedThreadClass = loadedState.threadClass
            try { selectedCoating = CoatingType.valueOf(loadedState.coating) } catch (_: Exception) {}
            batchQty = loadedState.batchQty
            currencyIsUsd = loadedState.currencyIsUsd
            cncHourRate = loadedState.cncHourRate
            htRatePerKg = loadedState.htRatePerKg
            marginPct = loadedState.marginPct
            try { selectedFontOption = com.aistudio.aerofast.trmst.ui.theme.AppFontOption.valueOf(loadedState.fontOption) } catch (_: Exception) {}
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

    // Active Fastener Calculation
    val currentCalculation = remember(
        selectedFastenerType,
        selectedUnitSystem,
        selectedStandard,
        selectedSizeIndex,
        selectedPitchValue,
        selectedLength,
        customThreadLengthMm,
        selectedThreadClass,
        activeMaterial,
        selectedCoating,
        batchQty,
        currencyIsUsd,
        cncHourRate,
        htRatePerKg,
        marginPct,
        selectedPropClass,
        appliedExtractedSpec,
        com.aistudio.aerofast.trmst.ui.components.ManualFlange.diaMm, com.aistudio.aerofast.trmst.ui.components.ManualFlange.dc,
        com.aistudio.aerofast.trmst.ui.components.ManualFlange.k, com.aistudio.aerofast.trmst.ui.components.ManualFlange.s, com.aistudio.aerofast.trmst.ui.components.ManualFlange.c
    ) {
        val sizingD = appliedExtractedSpec?.nominalDiaMm ?: FastenerCatalog.diameterMm(selectedUnitSystem, selectedSizeIndex)
        val flange = selectedFastenerType == FastenerType.FLANGE_BOLT
        val mf = com.aistudio.aerofast.trmst.ui.components.ManualFlange
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
            propertyClass = selectedPropClass,
            // Dimensions from the applied drawing; a flange bolt can also take its head sizes typed on the Sizing card
            drawing = (appliedExtractedSpec?.geometry() ?: com.aistudio.aerofast.trmst.data.DrawingGeometry()).let { g ->
                // A thread length typed on the Sizing card overrides the scanned one
                if (customThreadLengthMm > 0.0) g.copy(threadLengthMm = customThreadLengthMm) else g
            }.let { g ->
                if (!flange) g else g.copy(
                    acrossFlatsMm = g.acrossFlatsMm ?: mf.sFor(sizingD), headHeightMm = g.headHeightMm ?: mf.kFor(sizingD),
                    flangeDiaMm = g.flangeDiaMm ?: mf.dcFor(sizingD), flangeThicknessMm = g.flangeThicknessMm ?: mf.cFor(sizingD)
                )
            }
        )
    }

    fun reloadAppState() {
        scope.launch {
            val loadedState = AppStateStore.load(context)
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
                customThreadLengthMm = loadedState.customThreadLengthMm
                try { selectedPropClass = PropertyClass.valueOf(loadedState.propClass) } catch (_: Exception) {}
                selectedMaterialKey = loadedState.materialKey
                selectedThreadClass = loadedState.threadClass
                try { selectedCoating = CoatingType.valueOf(loadedState.coating) } catch (_: Exception) {}
                batchQty = loadedState.batchQty
                currencyIsUsd = loadedState.currencyIsUsd
                cncHourRate = loadedState.cncHourRate
                htRatePerKg = loadedState.htRatePerKg
                marginPct = loadedState.marginPct
                try { selectedFontOption = com.aistudio.aerofast.trmst.ui.theme.AppFontOption.valueOf(loadedState.fontOption) } catch (_: Exception) {}
                currentTier = loadedState.currentTier
                savedChatMessages = loadedState.chatMessages
                Toast.makeText(context, "State refreshed", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "State refreshed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    androidx.compose.runtime.CompositionLocalProvider(com.aistudio.aerofast.trmst.util.LocalAppLanguage provides currentLanguage) {
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
                        // Watched on the Initial pass so a pinch works anywhere, but a gesture is only
                        // CLAIMED once the fingers have actually spread or closed past PINCH_SLOP. Two
                        // fingers moving together is a scroll, and it has to reach the list underneath:
                        // consuming every two-finger event turned every two-finger scroll into a zoom.
                        awaitEachGesture {
                            var zooming = false
                            do {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                val pressedPointers = event.changes.filter { it.pressed }
                                val zoomChange = event.calculateZoom()
                                val panChange = event.calculatePan()

                                if (pressedPointers.size >= 2) {
                                    // Claim the gesture on the first real scale change, then keep it
                                    // for the rest of the gesture so the zoom does not stutter.
                                    if (!zooming && kotlin.math.abs(zoomChange - 1.0f) > PINCH_SLOP) zooming = true
                                    if (zooming) {
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
                                    // Only drag the zoomed view once it IS zoomed; at 1x a one-finger
                                    // drag belongs to whatever is underneath.
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
                        saveAppState()
                    },
                    fontOption = selectedFontOption,
                    onFontChanged = {
                        selectedFontOption = it
                        saveAppState()
                    },
                    currencyIsUsd = currencyIsUsd,
                    onCurrencyToggled = {
                        currencyIsUsd = it
                        saveAppState()
                    },
                    currentTier = currentTier,
                    onOpenPricing = { showPricingDialog = true },
                    onOpenSettings = { showSettingsDialog = true },
                    onRefresh = { reloadAppState() },
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
                                    customThreadLengthMm = 0.0
                                    selectedStandard = com.aistudio.aerofast.trmst.data.PokaYokeEngine.defaultStandard(newType, selectedUnitSystem)
                                    selectedThreadClass = SelectionFilter.threadClasses(selectedUnitSystem, newType, selectedStandard).first()
                                    saveAppState()
                                },
                                selectedUnitSystem = selectedUnitSystem,
                                onUnitSystemSelected = { newUnit ->
                                    selectedUnitSystem = newUnit
                                    selectedSizeIndex = FastenerCatalog.defaultSizeIndex(newUnit)
                                    selectedPitchValue = if (newUnit == UnitSystem.METRIC) 1.50 else 16.0
                                    selectedLength = if (newUnit == UnitSystem.METRIC) 60.0 else 50.8
                                    customThreadLengthMm = 0.0
                                    // Metric ↔ inch also changes the governing standard (ISO/DIN ↔ ASME/ASTM)
                                    selectedStandard = com.aistudio.aerofast.trmst.data.PokaYokeEngine.defaultStandard(selectedFastenerType, newUnit)
                                    selectedThreadClass = SelectionFilter.threadClasses(newUnit, selectedFastenerType, selectedStandard).first()
                                    appliedExtractedSpec = null
                                    saveAppState()
                                },
                                selectedStandard = selectedStandard,
                                onStandardSelected = {
                                    selectedStandard = it
                                    appliedExtractedSpec = null
                                    saveAppState()
                                },
                                selectedSizeIndex = selectedSizeIndex,
                                onSizeIndexSelected = { idx ->
                                    selectedSizeIndex = idx
                                    customThreadLengthMm = 0.0
                                    if (selectedUnitSystem == UnitSystem.METRIC) {
                                        val m = FastenerCatalog.METRIC_SIZES.getOrElse(idx) { FastenerCatalog.METRIC_SIZES[FastenerCatalog.defaultSizeIndex(UnitSystem.METRIC)] }
                                        selectedPitchValue = m.pitches.first()
                                    } else {
                                        val inc = FastenerCatalog.INCH_SIZES.getOrElse(idx) { FastenerCatalog.INCH_SIZES[FastenerCatalog.defaultSizeIndex(UnitSystem.INCH)] }
                                        selectedPitchValue = inc.tpis.first().toDouble()
                                    }
                                    appliedExtractedSpec = null
                                    saveAppState()
                                },
                                selectedPitchValue = selectedPitchValue,
                                onPitchSelected = {
                                    selectedPitchValue = it
                                    appliedExtractedSpec = null
                                    saveAppState()
                                },
                                selectedLength = selectedLength,
                                onLengthSelected = {
                                    selectedLength = it
                                    saveAppState()
                                },
                                customThreadLengthMm = customThreadLengthMm.takeIf { it > 0.0 },
                                onCustomThreadLengthChanged = {
                                    customThreadLengthMm = it ?: 0.0
                                    saveAppState()
                                },
                                selectedPropClass = selectedPropClass,
                                onPropClassSelected = {
                                    selectedPropClass = it
                                    appliedExtractedSpec = null
                                    saveAppState()
                                },
                                selectedMaterialKey = selectedMaterialKey,
                                onMaterialSelected = {
                                    selectedMaterialKey = it
                                    appliedExtractedSpec = null
                                    saveAppState()
                                },
                                selectedThreadClass = selectedThreadClass,
                                onThreadClassSelected = {
                                    selectedThreadClass = it
                                    appliedExtractedSpec = null
                                    saveAppState()
                                },
                                selectedCoating = selectedCoating,
                                onCoatingSelected = {
                                    selectedCoating = it
                                    saveAppState()
                                },
                                batchQty = batchQty,
                                onBatchQtyChanged = {
                                    batchQty = it
                                    saveAppState()
                                },
                                appliedExtractedSpec = appliedExtractedSpec,
                                onClearCustomScan = {
                                    appliedExtractedSpec = null
                                    saveAppState()
                                }
                            )
                        }

                        NavigationTab.FORGING -> {
                            ForgingMetallurgyScreen(
                                result = currentCalculation
                            )
                        }

                        NavigationTab.SCANNER -> {
                            AiScannerScreen(
                                currentResult = currentCalculation,
                                scannedSpec = scannedSpec,
                                onScanned = { scannedSpec = it },
                                onAskCoPilot = { currentTab = NavigationTab.COPILOT },
                                onApplyExtractedSpec = { spec ->
                                    appliedExtractedSpec = spec
                                    selectedFastenerType = spec.fastenerType
                                    selectedUnitSystem = spec.unitSystem
                                    selectedSizeIndex = spec.nominalSizeIndex
                                    // The scanner reports pitch in mm; inch sizes are selected by TPI, so snap to the size's nearest standard TPI
                                    selectedPitchValue = if (spec.unitSystem == UnitSystem.INCH) {
                                        val tpis = FastenerCatalog.INCH_SIZES.getOrNull(spec.nominalSizeIndex)?.tpis.orEmpty()
                                        val tpi = if (spec.pitchValue in 0.01..5.0) 25.4 / spec.pitchValue else spec.pitchValue
                                        tpis.minByOrNull { kotlin.math.abs(it - tpi) }?.toDouble() ?: tpi
                                    } else spec.pitchValue
                                    selectedLength = spec.lengthMm
                                    // The drawing's own thread length takes over from any typed value
                                    customThreadLengthMm = 0.0
                                    selectedPropClass = spec.propertyClass
                                    selectedMaterialKey = spec.materialKey
                                    // Keep a scanned thread class only if it is valid for this unit and internal/external thread
                                    val scanClasses = ThreadClassCatalog.getOptions(spec.unitSystem, spec.fastenerType.isNut)
                                    selectedThreadClass = scanClasses.firstOrNull { it.equals(spec.threadClass, ignoreCase = false) }
                                        ?: scanClasses.firstOrNull { it.equals(spec.threadClass, ignoreCase = true) }
                                        ?: scanClasses.first()
                                    selectedCoating = spec.coating
                                    selectedStandard = com.aistudio.aerofast.trmst.data.PokaYokeEngine.defaultStandard(spec.fastenerType, spec.unitSystem)
                                    currentTab = NavigationTab.SIZING
                                    saveAppState(scannedBlueprintTag = "${spec.fastenerType.label} (${spec.materialKey}, M${spec.nominalDiaMm}mm)")
                                }
                            )
                        }

                        NavigationTab.COPILOT -> {
                            // The scanned drawing comes first; the Sizing selection is only the secondary input
                            AiCoPilotScreen(
                                currentResult = scanCalculation ?: currentCalculation,
                                extractedSpec = scannedSpec.takeIf { scanCalculation != null },
                                sizingResult = currentCalculation.takeIf { scanCalculation != null },
                                onOpenScanner = { currentTab = NavigationTab.SCANNER },
                                initialMessages = savedChatMessages,
                                onChatMessageSent = { updatedMsgs ->
                                    savedChatMessages = updatedMsgs
                                    saveAppState(chatMsgs = updatedMsgs)
                                }
                            )
                        }

                        NavigationTab.EIGHT_D -> {
                            EightDKnowledgeScreen(currentResult = currentCalculation)
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
    fontOption: com.aistudio.aerofast.trmst.ui.theme.AppFontOption = com.aistudio.aerofast.trmst.ui.theme.AppFontOption.ROBOTO_MONO,
    onFontChanged: (com.aistudio.aerofast.trmst.ui.theme.AppFontOption) -> Unit = {},
    currencyIsUsd: Boolean,
    onCurrencyToggled: (Boolean) -> Unit,
    currentTier: String,
    onOpenPricing: () -> Unit,
    onOpenSettings: () -> Unit,
    onRefresh: () -> Unit = {},
    currentLanguage: com.aistudio.aerofast.trmst.util.AppLanguage = com.aistudio.aerofast.trmst.util.LocalAppLanguage.current
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
                            text = com.aistudio.aerofast.trmst.util.AppLocalization.t("app_subtitle", currentLanguage),
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
                                text = "${themeMode.icon} ${com.aistudio.aerofast.trmst.util.AppLocalization.t("theme", currentLanguage)}",
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
                            val all = com.aistudio.aerofast.trmst.ui.theme.AppFontOption.values().toList()
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
                                text = "${fontOption.icon} ${com.aistudio.aerofast.trmst.util.AppLocalization.t("font", currentLanguage)}",
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
                                text = com.aistudio.aerofast.trmst.util.AppLocalization.t("sync", currentLanguage),
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
                                text = com.aistudio.aerofast.trmst.util.AppLocalization.t("key", currentLanguage),
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
    currentLanguage: com.aistudio.aerofast.trmst.util.AppLanguage = com.aistudio.aerofast.trmst.util.LocalAppLanguage.current
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
