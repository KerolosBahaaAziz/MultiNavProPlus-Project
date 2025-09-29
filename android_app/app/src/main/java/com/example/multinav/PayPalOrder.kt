package com.example.multinav

data class CreateOrderRequest(
    val intent: String,
    val purchase_units: List<PurchaseUnit>
)

data class PurchaseUnit(
    val amount: Amount
)

data class Amount(
    val currency_code: String,
    val value: String
)

data class CreateOrderResponse(
    val id: String,
    val status: String
)

data class CaptureOrderResponse(
    val id: String,
    val status: String
)