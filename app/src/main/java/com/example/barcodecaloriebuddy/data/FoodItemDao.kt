package com.example.barcodecaloriebuddy.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodItemDao {
    @Insert
    suspend fun insert(foodItem: FoodItem)

    @Delete
    suspend fun delete(foodItem: FoodItem)

    @Query("SELECT * FROM food_items WHERE date >= :startOfDay AND date < :endOfDay")
    fun getTodaysFoodItems(startOfDay: Long, endOfDay: Long): Flow<List<FoodItem>>

    @Query("SELECT * FROM food_items WHERE date >= :startOfWeek AND date < :endOfWeek ORDER BY date DESC")
    fun getWeeklyFoodItems(startOfWeek: Long, endOfWeek: Long): Flow<List<FoodItem>>
}
