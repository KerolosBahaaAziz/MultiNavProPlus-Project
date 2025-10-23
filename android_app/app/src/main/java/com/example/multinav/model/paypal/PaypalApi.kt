package com.example.multinav.model.paypal

import com.example.multinav.model.OrderRequest
import com.example.multinav.model.OrderResponse
import com.example.multinav.model.TokenResponse
import retrofit2.Call
import retrofit2.http.*

interface PayPalApi {

    @FormUrlEncoded
    @POST("v1/oauth2/token")
    fun getAccessToken(
        @Header("Authorization") authHeader: String,
        @Field("grant_type") grantType: String = "client_credentials"
    ): Call<TokenResponse>

    @POST("v2/checkout/orders")
    fun createOrder(
        @Header("Authorization") bearer: String,
        @Body orderRequest: OrderRequest
    ): Call<OrderResponse>
}
