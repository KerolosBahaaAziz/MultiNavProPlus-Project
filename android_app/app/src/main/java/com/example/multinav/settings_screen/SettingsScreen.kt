// app/src/main/java/com/example/multinav/settings/SettingsScreen.kt
package com.example.multinav.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallTopAppBar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onLogout: () -> Unit,
    onSubscribeNavigate: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            SmallTopAppBar(title = { Text("Settings") })
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                state.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                state.error != null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = state.error ?: "Unknown error")
                    }
                }

                else -> {
                    val displayName = listOf(state.firstName, state.lastName).joinToString(" ").trim()

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp, vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Avatar circle with initial
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = state.firstName.firstOrNull()?.uppercase() ?: "U",
                                color = Color.White,
                                style = MaterialTheme.typography.headlineLarge
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = if (displayName.isNotBlank()) displayName else "Unknown user",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        if (state.isPremium) {
                            Text(
                                text = "Premium user",
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            // Subscribe -> navigate to subscription flow (PayPal/Stripe)
                            Button(
                                onClick = onSubscribeNavigate,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(text = "Subscribe")
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Dev helper: mark paid locally in RTDB (for testing)
                            Button(
                                onClick = { viewModel.markPaidLocally() },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                            ) {
                                Text(text = "Mark Paid (dev)", color = Color.White)
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        // Logout button — uses viewModel.logout() then navigation callback
                        Button(
                            onClick = {
                                viewModel.logout()
                                onLogout()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text(text = "Logout", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}
