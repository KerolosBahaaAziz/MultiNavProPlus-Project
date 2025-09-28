// app/src/main/java/com/example/multinav/settings/SettingsViewModel.kt
package com.example.multinav.settings

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val firstName: String = "",
    val lastName: String = "",
    val isPremium: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null
)

class SettingsViewModel(
    private val auth: FirebaseAuth,
    private val databaseRef: DatabaseReference
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        ensureUserInDatabase()
        loadUserData()
    }

    private fun loadUserData() {
        val user = auth.currentUser
        if (user == null) {
            _uiState.value = SettingsUiState(
                isLoading = false,
                error = "User not logged in"
            )
            return
        }

        val emailKey = user.email?.toFirebaseKey() ?: run {
            _uiState.value = SettingsUiState(isLoading = false, error = "Invalid email")
            return
        }
        Log.d("SettingsVM", "Email = ${user.email}")
        Log.d("SettingsVM", "EmailKey = $emailKey")

        _uiState.value = _uiState.value.copy(isLoading = true, error = null)

        databaseRef.child("UsersDB").child(emailKey).get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    val firstName = snapshot.child("firstName").getValue(String::class.java) ?: ""
                    val lastName = snapshot.child("lastName").getValue(String::class.java) ?: ""
                    val paid = snapshot.child("paid").getValue(Boolean::class.java) ?: false

                    _uiState.value = SettingsUiState(
                        firstName = firstName,
                        lastName = lastName,
                        isPremium = paid,
                        isLoading = false
                    )
                } else {
                    _uiState.value = SettingsUiState(
                        isLoading = false,
                        error = "User data not found"
                    )
                }
            }
            .addOnFailureListener { exc ->
                _uiState.value = SettingsUiState(
                    isLoading = false,
                    error = exc.message ?: "Failed to read user data"
                )
            }
    }
    fun ensureUserInDatabase() {
        val user = auth.currentUser ?: return
        val safeEmail = user.email?.toFirebaseKey() ?: return

        val userRef = databaseRef.child("UsersDB").child(safeEmail)
        userRef.get().addOnSuccessListener { snapshot ->
            if (!snapshot.exists()) {
                val newUser = mapOf(
                    "firstName" to "",
                    "lastName" to "",
                    "paid" to false
                )
                userRef.setValue(newUser)
            }
        }
    }

    fun logout() {
        auth.signOut()
    }

    /**
     * Small helper to mark user as paid (subscribe). In a real app you would use a payment
     * flow and only set paid=true after verifying the transaction on your backend.
     */
    fun markPaidLocally() {
        val user = auth.currentUser ?: return
        val emailKey = user.email?.replace(".", "-") ?: return
        databaseRef.child("UsersDB").child(emailKey).child("paid").setValue(true)
            .addOnSuccessListener {
                // update local state
                _uiState.value = _uiState.value.copy(isPremium = true)
            }
            .addOnFailureListener {
                // optionally update error
                _uiState.value = _uiState.value.copy(error = it.message)
            }
    }

    private fun String.toFirebaseKey(): String {
        return this.replace(".", "-") // same rule you used when saving
    }

}


class SettingsViewModelFactory(
    private val auth: FirebaseAuth,
    private val databaseReference: DatabaseReference
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            return SettingsViewModel(auth, databaseReference) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
