package com.aistudio.aerofast.desktop

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.runtime.remember
import androidx.compose.ui.res.painterResource

fun main() = application {
    val state = rememberWindowState(width = 1360.dp, height = 900.dp)
    Window(onCloseRequest = ::exitApplication, state = state, title = "AeroFast Pro — by Parandama Pathipati",
        icon = painterResource("aerofast.png")) {
        val platform = remember { DesktopPlatform { window } }
        AeroFastDesktop(platform)
    }
}
