package com.example.barcodecaloriebuddy.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class ProductResponse(
    val code: String? = null,
    val product: Product? = null,
    val status: Int = 0,
    @SerialName("status_verbose")
    val statusVerbose: String? = null
)

@Serializable
data class Product(
    @SerialName("product_name")
    val productName: String? = null,
    val nutriments: Nutriments? = null,
    @SerialName("image_url")
    val imageUrl: String? = null
)

@Serializable
data class Nutriments(
    @SerialName("energy-kcal_100g")
    val energyKcal100g: JsonElement? = null
)
