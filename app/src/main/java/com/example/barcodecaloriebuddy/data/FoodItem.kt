package com.example.barcodecaloriebuddy.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "food_items")
data class FoodItem(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val calories: Int,
    val quantity: Int,
    val imageUrl: String? = null,
    val isFavorite: Boolean = false,
    val caloriesPer100g: Int? = null,
    val isArchived: Boolean = false,
    @ColumnInfo(name = "barcode")
    val barcode: String? = null,
    val date: Long = System.currentTimeMillis()
)
