package com.example.multinav.model

import android.os.Build
import androidx.annotation.RequiresApi
import retrofit2.Call
import java.util.Base64

class PayPalRepository {
    private val clientId = "AfV273S3XkcsDv3NMNvTZONz1n-LZIJfe03Ada4pLqFkLBpZAQbm4GTP-6q_cDbAcuGYSndkeUEGq1VX"
    private val clientSecret = "EApDbDzkIVq-0y_BSyqtXi5j7aa5AYbsMlVjpI-3viOt28iZyIEJj8fRnyKo3D--wRvlkDzhR8sQBoXS"
    private val api = PayPalClient.api

    @RequiresApi(Build.VERSION_CODES.O)
    fun createOrder(amount: String): Call<OrderResponse> {
        // Get access token first
        val credentials = "$clientId:$clientSecret"
        val base64Credentials = Base64.getEncoder().encodeToString(credentials.toByteArray())
        val authHeader = "Basic $base64Credentials"

        return api.getAccessToken(authHeader).execute().let { tokenResponse ->
            if (tokenResponse.isSuccessful) {
                val token = tokenResponse.body()?.access_token
                val bearer = "Bearer $token"

                // Create order with the token
                val orderRequest = OrderRequest(
                    purchase_units = listOf(
                        PurchaseUnit(
                            amount = Amount(
                                currency_code = "USD",
                                value = amount
                            )
                        )
                    )
                )

                api.createOrder(bearer, orderRequest)
            } else {
                throw Exception("Failed to get access token")
            }
        }
    }
}