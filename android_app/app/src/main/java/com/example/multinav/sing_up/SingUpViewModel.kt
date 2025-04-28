package com.example.multinav.sing_up

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class SingUpViewModel(
    private val auth: FirebaseAuth
) : ViewModel() {
    var firstName by  mutableStateOf("")
    var lastName by  mutableStateOf("")
    var email by  mutableStateOf("")
    var password by  mutableStateOf("")
    var passwordVisible by mutableStateOf(false)
    var emailError by mutableStateOf<String?>(null)
    var passwordError by mutableStateOf<String?>(null)
    var uiState by mutableStateOf<UiState>(UiState.Initial)
        private set
    var verificationMessage by mutableStateOf<String?>(null)



    fun validateInputs(updateUiState: Boolean = true): Boolean {
        emailError = null
        passwordError = null

        when {
            email.isBlank() -> {
                emailError = "Email cannot be empty"
                if (updateUiState) uiState = UiState.Error("Email cannot be empty")
                return false
            }
            password.isBlank() -> {
                passwordError = "Password cannot be empty"
                if (updateUiState) uiState = UiState.Error("Password cannot be empty")
                return false
            }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                emailError = "Invalid email format"
                if (updateUiState) uiState = UiState.Error("Invalid email format")
                return false
            }
            password.length < 6 -> {
                passwordError = "Password must be at least 6 characters"
                if (updateUiState) uiState = UiState.Error("Password must be at least 6 characters")
                return false
            }
        }
        return true
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun sendVerificationEmail(user: FirebaseUser): Result<Void?> = suspendCancellableCoroutine { continuation ->
        user.sendEmailVerification()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    continuation.resume(Result.success(task.result))
                } else {
                    continuation.resumeWithException(task.exception ?: Exception("Failed to send verification email"))
                }

            }
    }
    fun singUp() {
        if (!validateInputs(updateUiState = true)) {
            return
        }
        uiState = UiState.Loading
        viewModelScope.launch {
            try {
                val authResult = auth.createUserWithEmailAndPassword(email, password).await()
                val user = authResult.user
                user?.let {
                    val result = sendVerificationEmail(it)
                    if (result.isSuccess) {
                        uiState = UiState.VerificationPending // New state to indicate verification is pending
                        verificationMessage = "Verification email sent to $email. Please verify your email to complete sign-up."
                        auth.signOut() // Sign out to prevent login until verified
                    } else {
                        uiState = UiState.Error("Failed to send verification email: ${result.exceptionOrNull()?.message}")
                        user.delete() // Clean up the user if verification email fails
                    }
                } ?: run {
                    uiState = UiState.Error("User creation failed.")
                }
            } catch (e: Exception) {
                uiState = UiState.Error(e.message ?: "Sign up failed")
                Log.e("SingUp", "Sign up failed", e)
            }
        }
    }
    fun resendVerificationEmail() {
        val user = auth.currentUser
        if (user != null) {
            viewModelScope.launch {
                try {
                    val result = sendVerificationEmail(user)
                    if (result.isSuccess) {
                        verificationMessage = "Verification email resent to $email. Please check your inbox."
                    } else {
                        uiState = UiState.Error("Failed to resend verification email: ${result.exceptionOrNull()?.message}")
                    }
                } catch (e: Exception) {
                    uiState = UiState.Error("Failed to resend verification email: ${e.message}")
                }
            }
        } else {
            uiState = UiState.Error("No user is signed in to resend verification email.")
        }
    }

    fun checkEmailVerification(onVerified: () -> Unit) {
        val user = auth.currentUser
        if (user != null) {
            viewModelScope.launch {
                try {
                    user.reload().await() // Refresh user data
                    if (user.isEmailVerified) {
                        uiState = UiState.Success
                        verificationMessage = "Email verified successfully!"
                        onVerified() // Navigate to login or next screen
                    } else {
                        uiState = UiState.VerificationPending
                        verificationMessage = "Email not yet verified. Please check your inbox."
                    }
                } catch (e: Exception) {
                    uiState = UiState.Error("Failed to check email verification: ${e.message}")
                }
            }
        } else {
            uiState = UiState.Error("No user is signed in.")
        }
    }

sealed interface UiState {
    object Initial : UiState
    object Loading : UiState
    object Success : UiState
    object VerificationPending : UiState
    data class Error(val errorMessage: String) : UiState


}

}



class SingUpViewModelFactory(
    private val auth: FirebaseAuth
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SingUpViewModel::class.java)) {
            return SingUpViewModel(auth) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}