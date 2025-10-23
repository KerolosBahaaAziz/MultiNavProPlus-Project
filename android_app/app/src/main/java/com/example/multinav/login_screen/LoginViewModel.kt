package com.example.multinav.login_screen

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class LoginViewModel(
    private val auth: FirebaseAuth
) : ViewModel(){

    var username by mutableStateOf("")
    var password by  mutableStateOf("")
    var passwordVisible by mutableStateOf(false)
    var loginState by mutableStateOf<LoginState>(LoginState.Initial)
        private set
    var verificationMessage by mutableStateOf<String?>(null)

    fun login(){
        if (username.isBlank() || password.isBlank()) {
            loginState = LoginState.Error("Email and Password must not be empty")
            return
        }
        loginState = LoginState.Loading
        viewModelScope.launch {
            try {
                val authResult = auth.signInWithEmailAndPassword(username, password).await()
                val user = authResult.user
                if (user != null) {
                    if (user.isEmailVerified) {
                        loginState = LoginState.Success
                    } else {
                        loginState = LoginState.Error("Please verify your email before logging in.")
                        verificationMessage = "A verification email has been sent to $username. Please verify your email."
                        auth.signOut() // Sign out to prevent unauthorized access
                    }
                } else {
                    loginState = LoginState.Error("User not found")
                }
            } catch (e: Exception) {
                loginState = LoginState.Error(e.message ?: "Login failed")
            }
        }
    }
    fun resendVerificationEmail() {
        val user = auth.currentUser
        if (user != null && !user.isEmailVerified) {
            viewModelScope.launch {
                try {
                    user.sendEmailVerification().await()
                    verificationMessage = "Verification email resent to $username. Please check your inbox."
                } catch (e: Exception) {
                    loginState = LoginState.Error("Failed to resend verification email: ${e.message}")
                }
            }
        } else {
            loginState = LoginState.Error("No user is signed in or email already verified.")
        }
    }


    sealed interface LoginState {
        object Initial : LoginState
        object Loading : LoginState
        object Success : LoginState
        data class Error(val message: String) : LoginState


    }
    fun resetState() {
        username = ""
        password = ""
        passwordVisible = false
        loginState = LoginState.Initial
        verificationMessage = null
    }
}



class LoginViewModelFactory(private val auth: FirebaseAuth) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            return LoginViewModel(auth) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}