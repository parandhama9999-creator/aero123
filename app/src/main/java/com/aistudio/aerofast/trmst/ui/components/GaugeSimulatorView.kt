package com.aistudio.aerofast.trmst.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.aerofast.trmst.data.CalculationEngine
import com.aistudio.aerofast.trmst.data.GaugeStatus
import com.aistudio.aerofast.trmst.ui.theme.AlertRed
import com.aistudio.aerofast.trmst.ui.theme.LocalCustomColors
import com.aistudio.aerofast.trmst.ui.theme.SuccessGreen
import com.aistudio.aerofast.trmst.ui.theme.WarningAmber
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun GaugeSimulatorView(
    targetBlankDia: Double,
    upperLimit: Double,
    lowerLimit: Double,
    tolHalf: Double,
    modifier: Modifier = Modifier
) {
    val customColors = LocalCustomColors.current

    var measuredValue by remember(targetBlankDia) { mutableFloatStateOf(targetBlankDia.toFloat()) }
    var textInput by remember(targetBlankDia) { mutableStateOf(targetBlankDia.toString()) }

    val span = max(0.04f, (tolHalf * 2.4).toFloat())
    val minSlider = (targetBlankDia - span).toFloat()
    val maxSlider = (targetBlankDia + span).toFloat()

    val status = CalculationEngine.evaluateShopGauge(
        measured = measuredValue.toDouble(),
        target = targetBlankDia,
        upper = upperLimit,
        lower = lowerLimit,
        tolHalf = tolHalf
    )

    val (badgeBg, badgeText) = when (status) {
        GaugeStatus.IDEAL_PASS -> SuccessGreen to "IDEAL PASS (94% Fill)"
        GaugeStatus.TIGHT_HIGH -> WarningAmber to "TIGHT / HIGH"
        GaugeStatus.LOOSE_LOW -> WarningAmber to "LOOSE / LOW"
        GaugeStatus.SCRAP_GO_BINDS -> AlertRed to "SCRAP (GO Gauge Binds)"
        GaugeStatus.SCRAP_NO_GO_FAILS -> AlertRed to "SCRAP (NO-GO > 1.5 Turns)"
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(customColors.bgElevated, RoundedCornerShape(10.dp))
            .border(1.dp, customColors.border, RoundedCornerShape(10.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Speed,
                    contentDescription = "Gauge",
                    tint = customColors.accentPrimary,
                    modifier = Modifier.padding(end = 6.dp)
                )
                Text(
                    text = "Live Shop Gauge Simulator:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = customColors.textMain
                )
            }

            // Status Badge
            Surface(
                color = badgeBg,
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = badgeText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Slider(
                value = measuredValue.coerceIn(minSlider, maxSlider),
                onValueChange = {
                    measuredValue = it
                    textInput = (((it * 1000).roundToInt()) / 1000.0).toString()
                },
                valueRange = minSlider..maxSlider,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = customColors.accentPrimary,
                    activeTrackColor = customColors.accentPrimary
                )
            )

            OutlinedTextField(
                value = textInput,
                onValueChange = { input ->
                    textInput = input
                    val parsed = input.toFloatOrNull()
                    if (parsed != null) {
                        measuredValue = parsed
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.width(95.dp),
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = customColors.textMain
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = customColors.accentPrimary,
                    unfocusedBorderColor = customColors.border
                )
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Min NO-GO: ${lowerLimit}mm",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = AlertRed
            )
            Text(
                text = "Nominal: ${targetBlankDia}mm",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = customColors.accentPrimary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Max GO: ${upperLimit}mm",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = AlertRed
            )
        }
    }
}
