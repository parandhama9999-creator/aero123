package com.aistudio.aerofast.trmst.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.aerofast.trmst.data.HeaderChoice
import com.aistudio.aerofast.trmst.data.HeadingPlan
import com.aistudio.aerofast.trmst.ui.theme.AlertRed
import com.aistudio.aerofast.trmst.ui.theme.LocalCustomColors

/** Auto / 1-die 2-blow / Multi-station: the die design, wire, cut-off and weights all follow the machine chosen here. */
@Composable
fun HeaderChoiceSelector(choice: HeaderChoice, onChoice: (HeaderChoice) -> Unit, plan: HeadingPlan?, modifier: Modifier = Modifier) {
    val c = LocalCustomColors.current
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("HEADING MACHINE", fontSize = 10.sp, fontWeight = FontWeight.Black, color = c.accentSecondary)
            HeaderChoice.values().forEach { option ->
                val selected = option == choice
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (selected) c.accentPrimary else c.bgElevated,
                    border = BorderStroke(1.dp, if (selected) c.accentPrimary else c.border),
                    modifier = Modifier.clickable { onChoice(option) }
                ) {
                    Text(
                        text = option.label, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                        color = if (selected) c.bgCard else c.textMain,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                    )
                }
            }
        }
        if (plan != null) {
            val blocked = plan.feasibilityIssue != null
            Text(
                text = "→ ${plan.machineDescription}: ${plan.routeReason}",
                fontSize = 10.sp,
                fontWeight = if (blocked) FontWeight.Bold else FontWeight.Normal,
                color = if (blocked) AlertRed else c.textMuted
            )
        }
    }
}
