package com.example.multinav.model.paypal


import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object PayPalPaymentManager {
    private val _paymentState = MutableStateFlow<PaymentState>(PaymentState.Idle)
    val paymentState: StateFlow<PaymentState> = _paymentState

    fun onPaymentSuccess(payerId: String?, paymentId: String?) {
        _paymentState.value = PaymentState.Success(payerId, paymentId)
    }

    fun onPaymentCancelled() {
        _paymentState.value = PaymentState.Cancelled
    }

    fun resetState() {
        _paymentState.value = PaymentState.Idle
    }
}

sealed class PaymentState {
    object Idle : PaymentState()
    data class Success(val payerId: String?, val paymentId: String?) : PaymentState()
    object Cancelled : PaymentState()
}