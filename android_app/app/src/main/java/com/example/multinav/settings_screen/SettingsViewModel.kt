// app/src/main/java/com/example/multinav/settings/SettingsViewModel.kt
package com.example.multinav.settings

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
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
        val user = auth.currentUser ?: return
        val uid = user.uid

        _uiState.value = _uiState.value.copy(isLoading = true)

        // Try reading new UID-based path first
        databaseRef.child("UsersDB").child(uid).get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    updateUiFromSnapshot(snapshot, user.email ?: "")
                } else {
                    // fallback for old email-based users
                    val oldEmailKey = user.email?.toFirebaseKey()
                    if (oldEmailKey != null) {
                        databaseRef.child("UsersDB").child(oldEmailKey).get()
                            .addOnSuccessListener { oldSnap ->
                                if (oldSnap.exists()) {
                                    // Migrate to UID path automatically
                                    val data = oldSnap.value
                                    databaseRef.child("UsersDB").child(uid).setValue(data)
                                    oldSnap.ref.removeValue() // optional cleanup
                                    updateUiFromSnapshot(oldSnap, user.email ?: "")
                                } else {
                                    createNewUser(uid)
                                }
                            }
                    } else {
                        createNewUser(uid)
                    }
                }
            }
            .addOnFailureListener {
                _uiState.value = _uiState.value.copy(isLoading = false, error = it.message)
            }
    }

    private fun updateUiFromSnapshot(snapshot: DataSnapshot, email: String) {
        val firstName = snapshot.child("firstName").getValue(String::class.java) ?: ""
        val lastName = snapshot.child("lastName").getValue(String::class.java) ?: ""
        val paid = snapshot.child("paid").getValue(Boolean::class.java) ?: false

        _uiState.value = SettingsUiState(
            firstName = firstName,
            lastName = lastName,
            email = email,
            isPremium = paid,
            isLoading = false
        )
    }

    private fun createNewUser(uid: String) {
        val newUser = mapOf(
            "firstName" to "",
            "lastName" to "",
            "paid" to false
        )
        databaseRef.child("UsersDB").child(uid).setValue(newUser)
        _uiState.value = _uiState.value.copy(isLoading = false)
    }
    fun ensureUserInDatabase() {
        val user = auth.currentUser ?: return
        val uid = user.uid

        val userRef = databaseRef.child("UsersDB").child(uid)
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

    fun changePassword(newPassword: String, onResult: (success: Boolean, message: String) -> Unit) {
        val user = auth.currentUser
        if (user != null) {
            user.updatePassword(newPassword)
                .addOnSuccessListener {
                    onResult(true, "Password updated successfully.")
                }
                .addOnFailureListener { e ->
                    onResult(false, e.message ?: "Failed to update password.")
                }
        } else {
            onResult(false, "No user is signed in.")
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

    fun markPaidAfterPayment() {
        val user = auth.currentUser ?: return
        val uid = user.uid

        databaseRef.child("UsersDB").child(uid).child("paid").setValue(true)
            .addOnSuccessListener {
                _uiState.value = _uiState.value.copy(isPremium = true)
            }
            .addOnFailureListener {
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
