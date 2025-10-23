package com.example.multinav.model

// TokenResponse.kt
data class TokenResponse(
    val access_token: String,
    val token_type: String,
    val expires_in: Int
)

// Amount.kt
data class Amount(
    val currency_code: String,
    val value: String
)

// PurchaseUnit.kt
data class PurchaseUnit(
    val amount: Amount
)



// OrderResponse.kt
data class OrderResponse(
    val id: String,
    val status: String,
    val links: List<LinkDescription>
)

data class LinkDescription(
    val href: String,
    val rel: String,
    val method: String
)
data class ApplicationContext(
    val return_url: String = "multinav://payment",
    val cancel_url: String = "multinav://payment"
)

data class OrderRequest(
    val intent: String = "CAPTURE",
    val purchase_units: List<PurchaseUnit>,
    val application_context: ApplicationContext = ApplicationContext()
)