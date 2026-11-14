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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * The private build's gate: sign in, then confirm the server granted this account the admin claim.
 *
 * The claim check here is a courtesy to the UI. The claim that matters is the one every admin
 * Cloud Function re-reads from the token server-side, so a patched build that skips this screen
 * still cannot mint, revoke or unbind anything.
 */
@Composable
fun AppGate(content: @Composable () -> Unit) {
    var signedIn by remember { mutableStateOf<Boolean?>(null) }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    suspend fun refreshClaim() {
        val user = FirebaseAuth.getInstance().currentUser
        signedIn = if (user == null) false else {
            val token = suspendCancellableCoroutine { cont ->
                user.getIdToken(true)
                    .addOnSuccessListener { cont.resume(it) }
                    .addOnFailureListener { cont.resumeWithException(it) }
            }
            token.claims["admin"] == true
        }
    }

    LaunchedEffect(Unit) { runCatching { refreshClaim() }.onFailure { signedIn = false } }

    when (signedIn) {
        null -> Centered { CircularProgressIndicator() }
        true -> content()
        false -> Centered {
            Text("AeroFast Admin", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = email,
                onValueChange = { email = it; error = null },
                label = { Text("Administrator email") },
                singleLine = true,
                enabled = !busy,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it; error = null },
                label = { Text("Password") },
                singleLine = true,
                enabled = !busy,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            error?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(16.dp))
            Button(
                enabled = !busy && email.isNotBlank() && password.isNotBlank(),
                onClick = {
                    busy = true; error = null
                    scope.launch {
                        try {
                            suspendCancellableCoroutine { cont ->
                                FirebaseAuth.getInstance()
                                    .signInWithEmailAndPassword(email.trim(), password)
                                    .addOnSuccessListener { cont.resume(Unit) }
                                    .addOnFailureListener { cont.resumeWithException(it) }
                            }
                            refreshClaim()
                            if (signedIn != true) {
                                error = "This account does not have administrator access."
                                FirebaseAuth.getInstance().signOut()
                            }
                        } catch (e: Exception) {
                            error = "Sign-in failed. Check the email and password."
                        } finally {
                            busy = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { if (busy) CircularProgressIndicator(Modifier.height(18.dp), strokeWidth = 2.dp) else Text("Sign in") }
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
