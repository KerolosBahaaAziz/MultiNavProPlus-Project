package com.example.multinav.sing_up

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class SingUpViewModel(
    private val auth: FirebaseAuth,
    private val database: FirebaseDatabase
) : ViewModel() {
    var firstName by mutableStateOf("")
    var lastName by mutableStateOf("")
    var email by mutableStateOf("")
    var password by mutableStateOf("")
    var passwordVisible by mutableStateOf(false)
    var firstNameError by mutableStateOf<String?>(null)
    var lastNameError by mutableStateOf<String?>(null)
    var emailError by mutableStateOf<String?>(null)
    var passwordError by mutableStateOf<String?>(null)
    var uiState by mutableStateOf<UiState>(UiState.Initial)
        private set
    var verificationMessage by mutableStateOf<String?>(null)

    fun validateInputs(updateUiState: Boolean = true): Boolean {
        firstNameError = null
        lastNameError = null
        emailError = null
        passwordError = null

        when {
            firstName.isBlank() -> {
                firstNameError = "First name cannot be empty"
                if (updateUiState) uiState = UiState.Error("First name cannot be empty")
                return false
            }
            !firstName.matches(Regex("^[A-Za-z]+$")) -> {
                firstNameError = "First name must contain only letters"
                if (updateUiState) uiState = UiState.Error("First name must contain only letters")
                return false
            }
        }

        // Change: Validate lastName
        when {
            lastName.isBlank() -> {
                lastNameError = "Last name cannot be empty"
                if (updateUiState) uiState = UiState.Error("Last name cannot be empty")
                return false
            }
            !lastName.matches(Regex("^[A-Za-z]+$")) -> {
                lastNameError = "Last name must contain only letters"
                if (updateUiState) uiState = UiState.Error("Last name must contain only letters")
                return false
            }
        }
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

    private fun sanitizeEmail(email: String): String {
        return email.replace(".", "-")
    }

    private suspend fun saveUserData(user: FirebaseUser) {
        val sanitizedEmail = sanitizeEmail(user.email ?: email)
        try {
            val userData = mapOf(
                "firstName" to firstName,
                "lastName" to lastName,
                "paid" to false,
            )
            database.reference
                .child(sanitizedEmail)
                .setValue(userData)
                .await()
        } catch (e: Exception) {
            uiState = UiState.Error("Failed to save user data: ${e.message}")
            Log.e("SingUp", "Failed to save user data", e)
        }
    }


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
                        uiState = UiState.VerificationPending
                        verificationMessage = "Verification email sent to $email. Please verify your email to complete sign-up."
                        // Change: Removed auth.signOut() to keep user signed in for verification check
                    } else {
                        uiState = UiState.Error("Failed to send verification email: ${result.exceptionOrNull()?.message}")
                        user.delete()
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
                        // Change: Set UiState to Success and update verification message
                        saveUserData(user)
                        uiState = UiState.Success
                        verificationMessage = "Email verified successfully!"
                        // Change: Sign out user after verification to prevent unauthorized access
                        auth.signOut()
                        // Change: Call onVerified to navigate to LoginScreen
                        onVerified()
                    } else {
                        // Change: Keep UiState as VerificationPending and provide feedback
                        uiState = UiState.VerificationPending
                        verificationMessage = "Email not yet verified. Please check your inbox or resend the verification email."
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
    private val auth: FirebaseAuth,
    private val database: FirebaseDatabase
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SingUpViewModel::class.java)) {
            return SingUpViewModel(auth,database) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}