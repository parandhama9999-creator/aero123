package com.aistudio.aerofast.trmst.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.aerofast.trmst.data.PokaYokeIssue
import com.aistudio.aerofast.trmst.data.PokaYokeSeverity
import com.aistudio.aerofast.trmst.ui.theme.LocalCustomColors

private val CriticalRed = Color(0xFFE11D48)
private val MandatoryAmber = Color(0xFFD97706)
private val WarningBlue = Color(0xFF2563EB)
private val PassGreen = Color(0xFF16A34A)

/** Strict poka-yoke gate: release status plus every failed rule with its fix. */
@Composable
fun PokaYokePanel(
    issues: List<PokaYokeIssue>,
    modifier: Modifier = Modifier
) {
    val critical = issues.count { it.severity == PokaYokeSeverity.CRITICAL }
    val mandatory = issues.count { it.severity == PokaYokeSeverity.MANDATORY }
    val warnings = issues.count { it.severity == PokaYokeSeverity.WARNING }
    var expanded by remember { mutableStateOf(true) }

    val (statusColor, statusText) = when {
        critical > 0 -> CriticalRed to "⛔ RELEASE BLOCKED — $critical critical poka-yoke failure${if (critical > 1) "s" else ""}"
        mandatory > 0 -> MandatoryAmber to "⚠️ RELEASE WITH $mandatory MANDATORY CONTROL${if (mandatory > 1) "S" else ""}"
        warnings > 0 -> WarningBlue to "ℹ️ OK — $warnings item${if (warnings > 1) "s" else ""} need engineering sign-off"
        else -> PassGreen to "✅ ALL POKA-YOKE CHECKS PASSED"
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = statusColor.copy(alpha = 0.08f),
        border = BorderStroke(1.5.dp, statusColor),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(statusColor, RoundedCornerShape(8.dp))
                    .clickable(enabled = issues.isNotEmpty()) { expanded = !expanded }
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Icon(
                    if (issues.isEmpty()) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = statusText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
                if (issues.isNotEmpty()) {
                    Text(if (expanded) "▲" else "▼", fontSize = 11.sp, color = Color.White)
                }
            }

            if (expanded) {
                issues.forEach { issue -> PokaYokeIssueRow(issue) }
            }
        }
    }
}

@Composable
private fun PokaYokeIssueRow(issue: PokaYokeIssue) {
    val customColors = LocalCustomColors.current
    val color = when (issue.severity) {
        PokaYokeSeverity.CRITICAL -> CriticalRed
        PokaYokeSeverity.MANDATORY -> MandatoryAmber
        PokaYokeSeverity.WARNING -> WarningBlue
    }
    Row(verticalAlignment = Alignment.Top) {
        Surface(shape = RoundedCornerShape(4.dp), color = color) {
            Text(
                text = issue.severity.name,
                fontSize = 8.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(text = issue.title, fontSize = 11.5.sp, fontWeight = FontWeight.Black, color = color)
            Text(text = issue.detail, fontSize = 10.5.sp, color = customColors.textMain, modifier = Modifier.padding(top = 1.dp))
            Text(text = "Fix: ${issue.fix}", fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, color = color, modifier = Modifier.padding(top = 2.dp))
        }
    }
}
