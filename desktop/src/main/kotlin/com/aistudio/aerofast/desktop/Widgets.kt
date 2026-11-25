package com.aistudio.aerofast.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.aerofast.trmst.data.AnnealStage
import com.aistudio.aerofast.trmst.ui.theme.LocalCustomColors

/** A labelled drop-down; long lists get a filter box at the top. */
@Composable
fun <T> Picker(label: String, options: List<T>, selected: T?, text: (T) -> String, onSelect: (T) -> Unit) {
    val c = LocalCustomColors.current
    var open by remember { mutableStateOf(false) }
    var filter by remember { mutableStateOf("") }
    Column(Modifier.fillMaxWidth()) {
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = c.textMuted)
        Box {
            Text(
                selected?.let(text) ?: "—", fontSize = 13.sp, color = c.textMain, maxLines = 2,
                modifier = Modifier.fillMaxWidth().background(c.bgCard, RoundedCornerShape(6.dp))
                    .border(1.dp, c.border, RoundedCornerShape(6.dp)).clickable { open = true; filter = "" }
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            )
            DropdownMenu(expanded = open, onDismissRequest = { open = false }, modifier = Modifier.heightIn(max = 480.dp).width(420.dp)) {
                if (options.size > 12) {
                    OutlinedTextField(filter, { filter = it }, placeholder = { Text("Filter", fontSize = 12.sp) }, singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp))
                }
                options.filter { filter.isBlank() || text(it).contains(filter.trim(), ignoreCase = true) }.forEach { o ->
                    DropdownMenuItem(text = { Text(text(o), fontSize = 13.sp) }, onClick = { onSelect(o); open = false })
                }
            }
        }
    }
}

@Composable
fun Card(title: String, accent: Color = LocalCustomColors.current.accentPrimary, content: @Composable () -> Unit) {
    val c = LocalCustomColors.current
    Surface(shape = RoundedCornerShape(10.dp), color = c.bgCard, border = androidx.compose.foundation.BorderStroke(1.dp, c.border), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.Black, color = accent)
            content()
        }
    }
}

@Composable
fun KV(label: String, value: String, valueColor: Color = LocalCustomColors.current.textMain) {
    val c = LocalCustomColors.current
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, fontSize = 12.sp, color = c.textMuted, modifier = Modifier.weight(0.38f))
        SelectionContainer(Modifier.weight(0.62f)) { Text(value, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = valueColor) }
    }
}

@Composable
fun Mono(text: String) {
    val c = LocalCustomColors.current
    SelectionContainer {
        Text(text, fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = c.textMain, lineHeight = 16.sp)
    }
}

@Composable
fun StageList(stages: List<AnnealStage>, total: String, basis: String, cautions: List<String>, sources: List<String>) {
    val c = LocalCustomColors.current
    stages.forEach { s ->
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(s.name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = c.textMain)
                Text(s.detail, fontSize = 11.sp, color = c.textMuted)
            }
            Text(s.hoursText, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = c.accentPrimary, modifier = Modifier.padding(start = 12.dp))
        }
    }
    KV("Total", total, c.accentSecondary)
    Text(basis, fontSize = 11.sp, color = c.textMuted)
    cautions.forEach { Text("⚠ $it", fontSize = 11.sp, color = Color(0xFFB45309)) }
    if (sources.isNotEmpty()) Text("Sources: " + sources.joinToString(" • "), fontSize = 10.sp, color = c.textMuted)
}
