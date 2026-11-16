package com.aistudio.aerofast.trmst.ui.components

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.aistudio.aerofast.trmst.data.FastenerCatalog
import com.aistudio.aerofast.trmst.data.HeatTreatmentSpec
import com.aistudio.aerofast.trmst.data.MaterialInfo
import com.aistudio.aerofast.trmst.ui.theme.AlertRed
import com.aistudio.aerofast.trmst.ui.theme.LocalCustomColors
import com.aistudio.aerofast.trmst.ui.theme.SuccessGreen

@Composable
fun MaterialCatalogDialog(
    selectedMaterialKey: String,
    onMaterialSelected: (String) -> Unit,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val customColors = LocalCustomColors.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }

    // Custom Material Form Fields
    var customKey by remember { mutableStateOf("") }
    var customName by remember { mutableStateOf("") }
    var customUts by remember { mutableStateOf("1050") }
    var customDensity by remember { mutableStateOf("7.85") }
    var customKm by remember { mutableStateOf("0.020") }
    var customYield by remember { mutableStateOf("940") }
    var customVc by remember { mutableStateOf("120") }
    var customHardness by remember { mutableStateOf("34 – 40 HRC") }
    var customStandard by remember { mutableStateOf("") }
    var customIsAero by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = customColors.bgCard,
            border = BorderStroke(1.dp, customColors.border),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = customColors.accentPrimary.copy(alpha = 0.15f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Science,
                                    contentDescription = "Materials",
                                    tint = customColors.accentPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Materials Catalog & Alloy Manager",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                color = customColors.textMain
                            )
                            Text(
                                text = "${FastenerCatalog.MATERIALS.size} registered alloys • Titanium, Superalloys, Steels",
                                fontSize = 11.sp,
                                color = customColors.textMuted
                            )
                        }
                    }

                    IconButton(onClick = onDismissRequest, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = customColors.textMuted)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Tabs: Catalog | Quick Presets | Add Custom
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = customColors.bgElevated,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = customColors.accentPrimary
                        )
                    }
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Catalog (${FastenerCatalog.MATERIALS.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Quick Presets", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("+ Custom Alloy", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                when (selectedTab) {
                    0 -> {
                        // Search bar
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search by name, key, or UTS (e.g. 4140, Inconel, 1040)...", fontSize = 11.sp) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = customColors.textMuted) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = customColors.accentPrimary,
                                unfocusedBorderColor = customColors.border
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        val allMaterials = FastenerCatalog.MATERIALS.values.toList()
                        val filtered = if (searchQuery.isBlank()) {
                            allMaterials
                        } else {
                            allMaterials.filter {
                                it.name.contains(searchQuery, ignoreCase = true) ||
                                it.key.contains(searchQuery, ignoreCase = true) ||
                                it.uts.toString().contains(searchQuery) ||
                                it.heatTreatmentSpec.standardReference.contains(searchQuery, ignoreCase = true)
                            }
                        }

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(filtered, key = { it.key }) { mat ->
                                val isSelected = mat.key == selectedMaterialKey
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) customColors.accentPrimary.copy(alpha = 0.12f) else customColors.bgElevated,
                                    border = BorderStroke(
                                        if (isSelected) 1.5.dp else 1.dp,
                                        if (isSelected) customColors.accentPrimary else customColors.border
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onMaterialSelected(mat.key)
                                            Toast.makeText(context, "Selected ${mat.name}", Toast.LENGTH_SHORT).show()
                                            onDismissRequest()
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = mat.name,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = customColors.textMain
                                                )
                                                if (mat.isAero) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = customColors.accentSecondary.copy(alpha = 0.2f)
                                                    ) {
                                                        Text(
                                                            text = "AERO",
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Black,
                                                            color = customColors.accentSecondary,
                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                        )
                                                    }
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "UTS: ${mat.uts.toInt()} MPa • Density: ${mat.density} g/cm³ • km: ${mat.km} • Std: ${mat.heatTreatmentSpec.standardReference}",
                                                fontSize = 10.sp,
                                                color = customColors.textMuted,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }

                                        if (isSelected) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = "Active",
                                                tint = SuccessGreen,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        } else {
                                            OutlinedButton(
                                                onClick = {
                                                    onMaterialSelected(mat.key)
                                                    Toast.makeText(context, "Selected ${mat.name}", Toast.LENGTH_SHORT).show()
                                                    onDismissRequest()
                                                },
                                                shape = RoundedCornerShape(6.dp),
                                                modifier = Modifier.height(28.dp)
                                            ) {
                                                Text("Select", fontSize = 10.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    1 -> {
                        // Quick Presets
                        Text(
                            text = "PRE-ENGINEERED SPECIALTY & AEROSPACE ALLOYS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = customColors.accentSecondary,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                        Text(
                            text = "Tap any preset below to register it into your active materials list:",
                            fontSize = 11.sp,
                            color = customColors.textMuted
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        val candidates = FastenerCatalog.getAvailableCustomCandidates()
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(candidates, key = { it.key }) { cand ->
                                val alreadyAdded = FastenerCatalog.MATERIALS.containsKey(cand.key)
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = customColors.bgElevated,
                                    border = BorderStroke(1.dp, customColors.border),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = cand.name,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = customColors.textMain
                                            )
                                            Text(
                                                text = "UTS: ${cand.uts.toInt()} MPa | Density: ${cand.density} g/cm³ | km: ${cand.km}",
                                                fontSize = 10.sp,
                                                color = customColors.accentPrimary
                                            )
                                            Text(
                                                text = cand.heatTreatmentSpec.standardReference,
                                                fontSize = 10.sp,
                                                color = customColors.textMuted
                                            )
                                        }

                                        Button(
                                            onClick = {
                                                FastenerCatalog.addMaterial(cand)
                                                com.aistudio.aerofast.trmst.data.CustomMaterialStore.savePreset(context, cand.key)
                                                onMaterialSelected(cand.key)
                                                Toast.makeText(context, "Added and selected ${cand.name}!", Toast.LENGTH_SHORT).show()
                                                onDismissRequest()
                                            },
                                            shape = RoundedCornerShape(6.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (alreadyAdded) SuccessGreen else customColors.accentPrimary
                                            ),
                                            modifier = Modifier.height(32.dp)
                                        ) {
                                            Icon(
                                                if (alreadyAdded) Icons.Default.Check else Icons.Default.Add,
                                                contentDescription = "Add",
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(if (alreadyAdded) "Select" else "Add", fontSize = 10.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    2 -> {
                        // Custom Alloy Form
                        val formScroll = rememberScrollState()
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(formScroll),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "ENTER CUSTOM ALLOY SPECIFICATIONS",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = customColors.accentSecondary
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = customKey,
                                    onValueChange = { customKey = it.uppercase().replace(" ", "_") },
                                    label = { Text("Alloy Code (e.g. INCONEL625)", fontSize = 10.sp) },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = customUts,
                                    onValueChange = { customUts = it },
                                    label = { Text("UTS (MPa)", fontSize = 10.sp) },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            OutlinedTextField(
                                value = customName,
                                onValueChange = { customName = it },
                                label = { Text("Full Material Name (e.g. Inconel 625 AMS 5666)", fontSize = 10.sp) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = customDensity,
                                    onValueChange = { customDensity = it },
                                    label = { Text("Density (g/cm³)", fontSize = 10.sp) },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = customKm,
                                    onValueChange = { customKm = it },
                                    label = { Text("km Blank Factor (0.01-0.03)", fontSize = 10.sp) },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = customYield,
                                    onValueChange = { customYield = it },
                                    label = { Text("Yield Strength (MPa)", fontSize = 10.sp) },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = customVc,
                                    onValueChange = { customVc = it },
                                    label = { Text("Cutting Speed Vc (m/min)", fontSize = 10.sp) },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            OutlinedTextField(
                                value = customHardness,
                                onValueChange = { customHardness = it },
                                label = { Text("Target Hardness", fontSize = 10.sp) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = customStandard,
                                onValueChange = { customStandard = it },
                                label = { Text("Material / Heat-Treatment Specification (e.g. AMS 5666)", fontSize = 10.sp) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                androidx.compose.material3.Checkbox(checked = customIsAero, onCheckedChange = { customIsAero = it })
                                Text("Aerospace-grade alloy (warm forming, aero NDT and cost model)", fontSize = 10.sp, color = customColors.textMain)
                            }

                            // Strict validation: nothing is registered until every value is physically plausible
                            val customInput = com.aistudio.aerofast.trmst.data.CustomMaterialInput(
                                key = customKey.trim(),
                                name = customName,
                                utsMpa = customUts.replace(',', '.').toDoubleOrNull() ?: -1.0,
                                yieldMpa = customYield.replace(',', '.').toDoubleOrNull() ?: -1.0,
                                density = customDensity.replace(',', '.').toDoubleOrNull() ?: -1.0,
                                km = customKm.replace(',', '.').toDoubleOrNull() ?: -1.0,
                                vc = customVc.replace(',', '.').toDoubleOrNull() ?: -1.0,
                                isAero = customIsAero,
                                hardness = customHardness,
                                standard = customStandard
                            )
                            val customErrors = com.aistudio.aerofast.trmst.data.CustomMaterialStore.validate(customInput)
                            if (customErrors.isNotEmpty()) {
                                Text(
                                    text = customErrors.joinToString("\n") { "⛔ $it" },
                                    fontSize = 10.sp,
                                    color = Color(0xFFE11D48),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Button(
                                enabled = customErrors.isEmpty(),
                                onClick = {
                                    val newMat = com.aistudio.aerofast.trmst.data.CustomMaterialStore.build(customInput)
                                    com.aistudio.aerofast.trmst.data.CustomMaterialStore.save(context, customInput)
                                    FastenerCatalog.addMaterial(newMat)
                                    onMaterialSelected(newMat.key)
                                    Toast.makeText(context, "Registered and saved ${newMat.name}", Toast.LENGTH_LONG).show()
                                    onDismissRequest()
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = customColors.accentPrimary),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add Custom Material to Catalog", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
