package com.example.barcodecaloriebuddy.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "food_items")
data class FoodItem(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val calories: Int,
    val quantity: Int, // in grams
    val date: Long = System.currentTimeMillis()
)
