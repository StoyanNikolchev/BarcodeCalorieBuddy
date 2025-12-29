package com.example.barcodecaloriebuddy.data

import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class FoodRepository(private val foodItemDao: FoodItemDao) {

    fun getTodaysFoodItems(startOfDay: Long, endOfDay: Long): Flow<List<FoodItem>> {
        return foodItemDao.getTodaysFoodItems(startOfDay, endOfDay)
    }

    fun getFavoriteFoodItems(): Flow<List<FoodItem>> {
        return foodItemDao.getFavoriteFoodItems()
    }

    fun getAllFoodItems(): Flow<List<FoodItem>> {
        return foodItemDao.getAllFoodItems()
    }

    suspend fun findMostRecentByBarcode(barcode: String): FoodItem? {
        return foodItemDao.findMostRecentByBarcode(barcode)
    }

    suspend fun addOrUpdateFoodItem(foodItem: FoodItem) {
        val startOfDay = getStartOfDayInMillis()
        val endOfDay = getEndOfDayInMillis()
        val existingItem = foodItemDao.findTodaysItemByName(foodItem.name, startOfDay, endOfDay)

        if (existingItem != null) {
            val updatedItem = existingItem.copy(
                calories = existingItem.calories + foodItem.calories,
                quantity = existingItem.quantity + foodItem.quantity,
                // If the new item has an image, use it. Otherwise, keep the old one.
                imageUrl = foodItem.imageUrl ?: existingItem.imageUrl 
            )
            foodItemDao.update(updatedItem)
        } else {
            foodItemDao.insert(foodItem)
        }
    }

    suspend fun deleteFoodItem(foodItem: FoodItem) {
        foodItemDao.delete(foodItem)
    }

    suspend fun updateFoodItem(foodItem: FoodItem) {
        foodItemDao.update(foodItem)
    }

    suspend fun countOtherEntries(name: String, id: Int): Int {
        return foodItemDao.countOtherEntries(name, id)
    }

    private fun getStartOfDayInMillis(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private fun getEndOfDayInMillis(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        return calendar.timeInMillis
    }
}
