package com.example.barcodecaloriebuddy.di

import android.content.Context
import com.example.barcodecaloriebuddy.data.AppDatabase
import com.example.barcodecaloriebuddy.data.FoodRepository

object Injection {
    fun provideFoodRepository(context: Context): FoodRepository {
        val database = AppDatabase.getDatabase(context)
        return FoodRepository(database.foodItemDao())
    }
}
