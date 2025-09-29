// app/src/main/java/com/example/multinav/settings/SettingsScreen.kt
package com.example.multinav

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.multinav.payment.PayPalApiService
import com.example.multinav.settings.SettingsViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onLogout: () -> Unit,
    onSubscribeNavigate: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var newPassword by remember { mutableStateOf("") }
    var showPasswordField by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var isProcessingPayment by remember { mutableStateOf(false) }
    val paypalService = remember { PayPalApiService() }

    val paymentState by PayPalPaymentManager.paymentState.collectAsState()


    // Handle payment state changes
    LaunchedEffect(paymentState) {
        when (paymentState) {
            is PaymentState.Success -> {
                // Payment successful - mark user as premium
                viewModel.markPaidAfterPayment()
                snackbarHostState.showSnackbar("Payment successful! You're now premium!")
                PayPalPaymentManager.resetState()
            }
            is PaymentState.Cancelled -> {
                snackbarHostState.showSnackbar("Payment was cancelled")
                PayPalPaymentManager.resetState()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = { SmallTopAppBar(title = { Text("Settings") }) },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
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

                        Spacer(modifier = Modifier.height(4.dp))

                        // Show email
                        Text(
                            text = state.email,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Premium status / subscribe
                        if (state.isPremium) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = "Premium",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Premium Member",
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        } else {
                            // PayPal payment button with dynamic order creation
                            Button(
                                onClick = {
                                    isProcessingPayment = true
                                    coroutineScope.launch {
                                        try {
                                            // Create a new order with PayPal API
                                            val orderId = paypalService.createOrder("9.99")

                                            // Open PayPal checkout with the new order ID
                                            val paypalUrl = "https://www.sandbox.paypal.com/checkoutnow?token=$orderId"
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(paypalUrl))
                                            context.startActivity(intent)

                                            // Show success message
                                            snackbarHostState.showSnackbar(
                                                "Complete payment in browser. Use 'Mark Paid' button after payment."
                                            )

                                            // Note: In a real app, you'd verify the payment on your backend
                                            // before marking as paid
                                        } catch (e: Exception) {
                                            snackbarHostState.showSnackbar(
                                                "Failed to create PayPal order: ${e.message}"
                                            )
                                        } finally {
                                            isProcessingPayment = false
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !isProcessingPayment,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF0070E0) // PayPal blue
                                )
                            ) {
                                if (isProcessingPayment) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text("Subscribe with PayPal ($9.99/month)")
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Dev button to mark as paid (for testing)
                            OutlinedButton(
                                onClick = {
                                    viewModel.markPaidLocally()
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Marked as premium (dev mode)")
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color(0xFF2E7D32)
                                ),
                                border = BorderStroke(1.dp, Color(0xFF2E7D32))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Build,
                                    contentDescription = "Dev",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "Mark Paid (Dev Testing)")
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Divider()

                        Spacer(modifier = Modifier.height(24.dp))

                        // Change Password Section
                        if (!showPasswordField) {
                            OutlinedButton(
                                onClick = { showPasswordField = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Password"
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Change Password")
                            }
                        } else {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Text(
                                        "Update Password",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    OutlinedTextField(
                                        value = newPassword,
                                        onValueChange = { newPassword = it },
                                        label = { Text("New Password") },
                                        placeholder = { Text("Enter at least 6 characters") },
                                        visualTransformation = PasswordVisualTransformation(),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                if (newPassword.length >= 6) {
                                                    viewModel.changePassword(newPassword) { success, message ->
                                                        coroutineScope.launch {
                                                            snackbarHostState.showSnackbar(message)
                                                            if (success) {
                                                                newPassword = ""
                                                                showPasswordField = false
                                                            }
                                                        }
                                                    }
                                                } else {
                                                    coroutineScope.launch {
                                                        snackbarHostState.showSnackbar(
                                                            "Password must be at least 6 characters"
                                                        )
                                                    }
                                                }
                                            },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Update")
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                newPassword = ""
                                                showPasswordField = false
                                            },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Cancel")
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        // Logout button
                        Button(
                            onClick = {
                                viewModel.logout()
                                onLogout()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.ExitToApp,
                                contentDescription = "Logout"
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Logout", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}