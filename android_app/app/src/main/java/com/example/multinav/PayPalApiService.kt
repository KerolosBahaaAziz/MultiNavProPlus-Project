package com.example.multinav.payment

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class PayPalApiService {
    companion object {
        // Replace with your PayPal Sandbox credentials
        private const val CLIENT_ID = "AfV273S3XkcsDv3NMNvTZONz1n-LZIJfe03Ada4pLqFkLBpZAQbm4GTP-6q_cDbAcuGYSndkeUEGq1VX"
        private const val CLIENT_SECRET = "EApDbDzkIVq-0y_BSyqtXi5j7aa5AYbsMlVjpI-3viOt28iZyIEJj8fRnyKo3D--wRvlkDzhR8sQBoXS"
        private const val BASE_URL = "https://api-m.sandbox.paypal.com"

        private val client = OkHttpClient()
        private val JSON = "application/json".toMediaType()
    }

    suspend fun createOrder(amount: String): String = withContext(Dispatchers.IO) {
        val accessToken = getAccessToken()

        val orderRequest = JSONObject().apply {
            put("intent", "CAPTURE")
            put("purchase_units", JSONArray().apply {
                put(JSONObject().apply {
                    put("amount", JSONObject().apply {
                        put("currency_code", "USD")
                        put("value", amount)
                    })
                    put("description", "Premium Subscription")
                })
            })
            put("application_context", JSONObject().apply {
                put("return_url", "https://example.com/return")
                put("cancel_url", "https://example.com/cancel")
            })
        }

        val request = Request.Builder()
            .url("$BASE_URL/v2/checkout/orders")
            .header("Authorization", "Bearer $accessToken")
            .header("Content-Type", "application/json")
            .post(orderRequest.toString().toRequestBody(JSON))
            .build()

        suspendCoroutine { continuation ->
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    continuation.resumeWithException(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        if (it.isSuccessful) {
                            val json = JSONObject(it.body!!.string())
                            val orderId = json.getString("id")
                            continuation.resume(orderId)
                        } else {
                            continuation.resumeWithException(Exception("Failed to create order: ${it.code}"))
                        }
                    }
                }
            })
        }
    }

    private suspend fun getAccessToken(): String = suspendCoroutine { continuation ->
        val credentials = "$CLIENT_ID:$CLIENT_SECRET"
        val auth = "Basic " + Base64.encodeToString(credentials.toByteArray(), Base64.NO_WRAP)

        val request = Request.Builder()
            .url("$BASE_URL/v1/oauth2/token")
            .header("Authorization", auth)
            .post("grant_type=client_credentials".toRequestBody("application/x-www-form-urlencoded".toMediaType()))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                continuation.resumeWithException(e)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (it.isSuccessful) {
                        val json = JSONObject(it.body!!.string())
                        continuation.resume(json.getString("access_token"))
                    } else {
                        continuation.resumeWithException(Exception("Failed to get access token"))
                    }
                }
            }
        })
    }
}