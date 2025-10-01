package com.example.multinav

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.multinav.model.paypal.PayPalPaymentManager
import com.example.multinav.model.paypal.PaymentState
import com.example.multinav.payment.PayPalApiService
import com.example.multinav.settings.SettingsViewModel
import com.example.multinav.ui.theme.AppTheme
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


    LaunchedEffect(paymentState) {
        when (paymentState) {
            is PaymentState.Success -> {
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
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(brush = Brush.horizontalGradient(AppTheme.gradientColors))
            ) {
                TopAppBar(
                    title = { Text("Settings") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = Color.White
                    )
                )
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF233992).copy(alpha = 0.95f),
                            Color(0xFFA030C7).copy(alpha = 0.95f),
                            Color(0xFF1C0090).copy(alpha = 0.95f)
                        )
                    )
                )
        ) {
            when {
                state.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color.Cyan)
                    }
                }
                state.error != null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = state.error ?: "Unknown error", color = Color.White)
                    }
                }
                else -> {
                    val displayName = listOf(state.firstName, state.lastName).joinToString(" ").trim()

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Avatar
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
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = state.email,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.7f)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Premium / Subscribe
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
                            Button(
                                onClick = {
                                    isProcessingPayment = true
                                    coroutineScope.launch {
                                        try {
                                            val orderId = paypalService.createOrder("9.99")
                                            val paypalUrl = "https://www.sandbox.paypal.com/checkoutnow?token=$orderId"
                                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(paypalUrl)))
                                            snackbarHostState.showSnackbar(
                                                "Complete payment in browser. Use 'Mark Paid' after payment."
                                            )
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
                                colors = ButtonDefaults.buttonColors(containerColor = AppTheme.gradientColors[1])
                            ) {
                                if (isProcessingPayment) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        // 🔹 Option 1: Default payment icon
                                        Icon(
                                            imageVector = Icons.Default.Payment, // or Icons.Default.CreditCard
                                            contentDescription = "PayPal",
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )

                                        // 🔹 Option 2: If you have PayPal logo drawable, use this instead:
                                        // Icon(
                                        //     painter = painterResource(id = R.drawable.paypal_logo),
                                        //     contentDescription = "PayPal",
                                        //     tint = Color.Unspecified, // keep original logo colors
                                        //     modifier = Modifier.size(20.dp)
                                        // )

                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Subscribe with PayPal", color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Change password
                        if (!showPasswordField) {
                            OutlinedButton(
                                onClick = { showPasswordField = true },
                                modifier = Modifier.fillMaxWidth(),
                                border = BorderStroke(1.dp, Brush.horizontalGradient(AppTheme.gradientColors)),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = Color.Black
                                )
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
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(6.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Text(
                                        "Update Password",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
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
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = AppTheme.gradientColors[0],
                                            unfocusedBorderColor = Color.Gray.copy(alpha = 0.4f),
                                            cursorColor = AppTheme.gradientColors[0],
                                            focusedTextColor = Color.Black,
                                            unfocusedTextColor = Color.Black.copy(alpha = 0.7f),
                                            focusedPlaceholderColor = Color.Gray,
                                            unfocusedPlaceholderColor = Color.Gray.copy(alpha = 0.6f)
                                        )
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // Gradient Update button
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
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                            contentPadding = PaddingValues()
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(brush = Brush.horizontalGradient(AppTheme.gradientColors))
                                                    .padding(vertical = 12.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text("Update", color = Color.White, fontWeight = FontWeight.Bold)
                                            }
                                        }

                                        // Cancel button
                                        OutlinedButton(
                                            onClick = {
                                                newPassword = ""
                                                showPasswordField = false
                                            },
                                            modifier = Modifier.weight(1f),
                                            border = BorderStroke(1.dp, Brush.horizontalGradient(AppTheme.gradientColors)),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                contentColor = AppTheme.gradientColors[1]
                                            )
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
