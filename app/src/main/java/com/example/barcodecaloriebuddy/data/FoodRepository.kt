package com.example.barcodecaloriebuddy.data

import kotlinx.coroutines.flow.Flow

class FoodRepository(private val foodItemDao: FoodItemDao) {

    fun getTodaysFoodItems(startOfDay: Long, endOfDay: Long): Flow<List<FoodItem>> {
        return foodItemDao.getTodaysFoodItems(startOfDay, endOfDay)
    }

    fun getWeeklyFoodItems(startOfWeek: Long, endOfWeek: Long): Flow<List<FoodItem>> {
        return foodItemDao.getWeeklyFoodItems(startOfWeek, endOfWeek)
    }

    suspend fun insertFoodItem(foodItem: FoodItem) {
        foodItemDao.insert(foodItem)
    }

    suspend fun deleteFoodItem(foodItem: FoodItem) {
        foodItemDao.delete(foodItem)
    }
}
