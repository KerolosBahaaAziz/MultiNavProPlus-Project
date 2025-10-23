package com.example.multinav.settings_screen


// MARK: - Model
data class Coupon(
    val firstName: String,
    val lastName: String,
    val paid: Boolean,
    val paymentMethod: PaymentMethod
)

// MARK: - Payment methods
sealed class PaymentMethod {
    object Free : PaymentMethod()
    data class Paypal(val transactionId: String) : PaymentMethod()
    data class Stripe(val transactionId: String) : PaymentMethod()
    data class ApplePay(val token: String) : PaymentMethod()
    object Unknown : PaymentMethod()
}

// MARK: - Factory
object CouponFactory {
    fun fromFirebase(data: Map<String, Any>): Coupon? {
        val firstName = data["firstName"] as? String ?: return null
        val lastName = data["lastName"] as? String ?: return null
        val paid = data["paid"] as? Boolean ?: false

        val paymentMethod = when {
            data["paypalId"] is String -> PaymentMethod.Paypal(data["paypalId"] as String)
            data["stripeId"] is String -> PaymentMethod.Stripe(data["stripeId"] as String)
            data["applePayToken"] is String -> PaymentMethod.ApplePay(data["applePayToken"] as String)
            paid -> PaymentMethod.Unknown
            else -> PaymentMethod.Free
        }

        return Coupon(
            firstName = firstName,
            lastName = lastName,
            paid = paid,
            paymentMethod = paymentMethod
        )
    }
}
