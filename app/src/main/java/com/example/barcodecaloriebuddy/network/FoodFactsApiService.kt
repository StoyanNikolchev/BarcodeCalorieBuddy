package com.example.barcodecaloriebuddy.network

import com.example.barcodecaloriebuddy.network.dto.ProductResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

interface FoodFactsApiService {
    suspend fun getProduct(barcode: String): ProductResponse

    companion object {
        private const val BASE_URL = "https://world.openfoodfacts.org/api/v2/product/"

        fun create(): FoodFactsApiService {
            val client = HttpClient(CIO) {
                install(ContentNegotiation) {
                    json(Json {
                        ignoreUnknownKeys = true
                        isLenient = true
                    })
                }
                defaultRequest {
                    header(HttpHeaders.UserAgent, "BarcodeCalorieBuddy - Android - Version 1.0")
                }
            }

            return object : FoodFactsApiService {
                override suspend fun getProduct(barcode: String): ProductResponse {
                    return client.get("$BASE_URL$barcode.json") {
                        parameter("fields", "product_name,nutriments,status,status_verbose,image_url")
                    }.body()
                }
            }
        }
    }
}
