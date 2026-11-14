package com.example.gate

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.license.ActivationScreen
import com.example.license.LicenseManager
import com.example.license.LicenseState

/**
 * The Play Store build's gate: a licence key, one device.
 *
 * The `admin` flavour supplies its own [AppGate] with the same signature, so MainActivity needs no
 * knowledge of which app it is in, and neither build contains the other's path.
 */
@Composable
fun AppGate(content: @Composable () -> Unit) {
    val context = LocalContext.current
    var state by remember { mutableStateOf<LicenseState>(LicenseState.Checking) }

    LaunchedEffect(Unit) { state = LicenseManager.verify(context) }

    when (val s = state) {
        is LicenseState.Checking -> Centered { CircularProgressIndicator() }

        is LicenseState.NeedsActivation -> ActivationScreen(onActivated = { state = it })

        is LicenseState.Active -> content()

        is LicenseState.Blocked -> Centered {
            Text(
                when (s.reason) {
                    "EXPIRED" -> "Licence Expired"
                    "REVOKED" -> "Licence Revoked"
                    "UNBOUND" -> "Device Not Registered"
                    else -> "Verification Needed"
                },
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(Modifier.height(10.dp))
            Text(s.message, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = {
                    LicenseManager.signOut(context)
                    state = LicenseState.NeedsActivation
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Enter a licence key") }
        }
    }
}

@Composable
private fun Centered(body: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) { body() }
}
