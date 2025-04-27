package com.example.multinav.sing_up

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class SingUpViewModel : ViewModel() {
    var firstName by  mutableStateOf("")
    var lastName by  mutableStateOf("")
    var email by  mutableStateOf("")
    var password by  mutableStateOf("")
    var passwordVisible by mutableStateOf(false)
    var emailError by mutableStateOf<String?>(null)
    var passwordError by mutableStateOf<String?>(null)
    var uiState by mutableStateOf<UiState>(UiState.Initial)
        private set

    private val auth = FirebaseAuth.getInstance()

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


    fun singUp(){
        if (!validateInputs(updateUiState = true)) {
            return
        }
        uiState = UiState.Loading
        viewModelScope.launch {
            auth.createUserWithEmailAndPassword(email,password)
                .addOnCompleteListener{ task->
                    if(task.isSuccessful){
                        uiState = UiState.Success
                        val user = auth.currentUser
                        Log.d("SingUP", "${user?.email}  ${user?.uid}")
                    }
                    else{
                        uiState = UiState.Error(task.exception?.message ?: "Sign up failed")
                        Log.e("SingUP", "Sign up failed", task.exception)
                    }
                }

        }
    }

sealed interface UiState {
    object Initial : UiState
    object Loading : UiState
    object Success : UiState
    data class Error(val errorMessage: String) : UiState


}
    private fun validateInputs(): Boolean {
        emailError = null
        passwordError = null

        when {
            email.isBlank() -> {
                emailError = "Email cannot be empty"
                uiState = UiState.Error("Email cannot be empty")
                return false
            }
            password.isBlank() -> {
                passwordError = "Password cannot be empty"
                uiState = UiState.Error("Password cannot be empty")
                return false
            }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                emailError = "Invalid email format"
                uiState = UiState.Error("Invalid email format")
                return false
            }
            password.length < 6 -> {
                passwordError = "Password must be at least 6 characters"
                uiState = UiState.Error("Password must be at least 6 characters")
                return false
            }
        }
        return true
    }
}
