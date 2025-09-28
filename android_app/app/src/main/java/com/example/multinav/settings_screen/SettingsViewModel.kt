// app/src/main/java/com/example/multinav/settings/SettingsViewModel.kt
package com.example.multinav.settings

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

        // RTDB key in your database uses '-' instead of '.' in emails
        val emailKey = user.email?.replace(".", "-") ?: run {
            _uiState.value = SettingsUiState(isLoading = false, error = "Invalid email")
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, error = null)

        // Use addOnSuccessListener / addOnFailureListener - we don't need to suspend
        viewModelScope.launch {
            databaseRef.child("PublicCoupons").child(emailKey).get()
                .addOnSuccessListener { snapshot ->
                    if (snapshot.exists()) {
                        val firstName = snapshot.child("firstName").getValue(String::class.java) ?: ""
                        val lastName = snapshot.child("lastName").getValue(String::class.java) ?: ""
                        val paid = snapshot.child("paid").getValue(Boolean::class.java) ?: false

                        _uiState.value = SettingsUiState(
                            firstName = firstName,
                            lastName = lastName,
                            isPremium = paid,
                            isLoading = false,
                            error = null
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
        databaseRef.child("PublicCoupons").child(emailKey).child("paid").setValue(true)
            .addOnSuccessListener {
                // update local state
                _uiState.value = _uiState.value.copy(isPremium = true)
            }
            .addOnFailureListener {
                // optionally update error
                _uiState.value = _uiState.value.copy(error = it.message)
            }
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
