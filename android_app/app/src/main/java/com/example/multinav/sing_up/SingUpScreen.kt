package com.example.multinav.sing_up

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

@Composable
fun SingUpScreen(
    modifier: Modifier = Modifier,
    auth: FirebaseAuth,
    database: FirebaseDatabase,
    navigateToLogin: () -> Unit) {
    val viewModel: SingUpViewModel = viewModel(factory = SingUpViewModelFactory(auth))
    val uiState = viewModel.uiState
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top // Changed to Top to accommodate back button
    ){
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.Start
        ) {
            IconButton(onClick = { navigateToLogin() }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to Login",
                    tint = Color.Black
                )
            }
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ){
            Text(
                text = "Create Account",
                fontSize = 28.sp,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(32.dp))
            OutlinedTextField(
                value = viewModel.firstName,
                onValueChange = { viewModel.firstName = it },
                label = { Text("First Name") },
                leadingIcon = { Icon(Icons.Filled.PersonOutline, contentDescription = "First Name") },
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                )
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = viewModel.lastName,
                onValueChange = { viewModel.lastName = it },
                label = { Text("Last Name") },
                leadingIcon = { Icon(Icons.Filled.PersonOutline, contentDescription = "Last Name") },
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                )
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = viewModel.email,
                onValueChange = { viewModel.email = it },
                label = { Text("Email") },
                leadingIcon = { Icon(Icons.Filled.MailOutline, contentDescription = "Email") },
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors( // Use TextFieldDefaults.colors()
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                )
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = viewModel.password,
                onValueChange = { viewModel.password = it },
                label = { Text("password") },
                visualTransformation = if (viewModel.passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    val imageVector = if (viewModel.passwordVisible)
                        Icons.Filled.Visibility
                    else Icons.Filled.VisibilityOff

                    val description = if (viewModel.passwordVisible) "Hide password" else "Show password"

                    IconButton(onClick = {viewModel.passwordVisible = !viewModel.passwordVisible }) {
                        Icon(imageVector = imageVector, contentDescription = description)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors( // Use TextFieldDefaults.colors()
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                )
            )

            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    viewModel.singUp()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState != SingUpViewModel.UiState.VerificationPending
            ) {
                Text("Sign Up")
            }
            when (uiState) {
                is SingUpViewModel.UiState.Loading -> {
                    CircularProgressIndicator()
                }
                is SingUpViewModel.UiState.VerificationPending -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = viewModel.verificationMessage ?: "Please verify your email.",
                            color = Color.Blue
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = { viewModel.resendVerificationEmail() }) {
                            Text("Resend Verification Email")
                        }
                        TextButton(onClick = { viewModel.checkEmailVerification(navigateToLogin) }) {
                            Text("I've Verified My Email")
                        }
                    }
                }

                is SingUpViewModel.UiState.Success -> {
                    Text(viewModel.verificationMessage ?: "Sign up successful! ", color = Color.Green)
                    navigateToLogin()
                }
                is SingUpViewModel.UiState.Error -> {
                    Text("Error: ${uiState.errorMessage}", color = Color.Red)
                }
                else -> {} // Initial state
            }

        }

    }


}