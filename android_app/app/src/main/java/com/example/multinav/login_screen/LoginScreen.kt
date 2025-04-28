package com.example.multinav.login_screen

import androidx.compose.foundation.clickable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Person
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

@Composable
fun LoginScreen(
    modifier: Modifier = Modifier,
    auth: FirebaseAuth,
    navigateToSignUp: () -> Unit,
    navigateToMainScreen: () -> Unit) {
    val viewModel: LoginViewModel = viewModel(factory = LoginViewModelFactory(auth))
    val loginState  = viewModel.loginState
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ){
        Text(
            text = "Login Page",
            fontSize = 28.sp,
            color = Color.Black
        )
        Spacer(modifier = Modifier.height(32.dp))
        OutlinedTextField(
            value = viewModel.username,
            onValueChange = { viewModel.username = it },
            label = {
                Text("username")
                    },
            leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = "Username") },
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

                IconButton(onClick = { viewModel.passwordVisible = !viewModel.passwordVisible }) {
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
                viewModel.login()

                println("Username: ${viewModel.username}, Password: ${viewModel.password}")
            },
            modifier = Modifier.fillMaxWidth(),

        ) {
            Text("Login")
        }
        Spacer(modifier = Modifier.height(24.dp))
        Row {
            Text(
                text = "Don’t have an account?",
                color = Color.Black,
                modifier = Modifier.clickable {
                    navigateToSignUp()
                }

                )
            Text(
                text = "Sign Up",
                color = Color.Blue,
                modifier = Modifier.clickable {
                    navigateToSignUp()
                }
                )
        }
        Spacer(modifier = Modifier.height(24.dp))
        when (loginState ) {
            is LoginViewModel.LoginState.Loading -> {
                CircularProgressIndicator()
            }
            is LoginViewModel.LoginState.Success -> {
                Text("Login Successful!", color = Color.Green)
                navigateToMainScreen()
            }
            is LoginViewModel.LoginState.Error -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = (loginState as LoginViewModel.LoginState.Error).message,
                        color = Color.Red
                    )
                    if (loginState.message.contains("verify your email", ignoreCase = true)) {
                        viewModel.verificationMessage?.let { message ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = message,
                                color = Color.Blue
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(onClick = { viewModel.resendVerificationEmail() }) {
                                Text("Resend Verification Email")
                            }
                        }
                    }
                }
            }
            else -> {}
        }



    }

}