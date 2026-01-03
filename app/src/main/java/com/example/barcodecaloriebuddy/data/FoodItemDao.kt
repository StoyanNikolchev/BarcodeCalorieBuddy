package com.example.barcodecaloriebuddy.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodItemDao {
    @Insert
    suspend fun insert(foodItem: FoodItem)

    @Delete
    suspend fun delete(foodItem: FoodItem)

    @Update
    suspend fun update(foodItem: FoodItem)

    @Query("SELECT * FROM food_items WHERE isArchived = 0 AND date >= :startOfDay AND date < :endOfDay ORDER BY date DESC")
    fun getTodaysFoodItems(startOfDay: Long, endOfDay: Long): Flow<List<FoodItem>>

    @Query("SELECT * FROM food_items WHERE isFavorite = 1 GROUP BY name ORDER BY date DESC")
    fun getFavoriteFoodItems(): Flow<List<FoodItem>>

    @Query("SELECT * FROM food_items WHERE isArchived = 0 ORDER BY date DESC")
    fun getAllFoodItems(): Flow<List<FoodItem>>

    @Query("SELECT COUNT(*) FROM food_items WHERE name = :name AND id != :id")
    suspend fun countOtherEntries(name: String, id: Int): Int

    @Query("SELECT * FROM food_items WHERE barcode = :barcode ORDER BY date DESC LIMIT 1")
    suspend fun findMostRecentByBarcode(barcode: String): FoodItem?

    @Query("SELECT * FROM food_items WHERE isArchived = 0 AND name = :name AND date >= :startOfDay AND date < :endOfDay LIMIT 1")
    suspend fun findTodaysItemByName(name: String, startOfDay: Long, endOfDay: Long): FoodItem?

    @Query("UPDATE food_items SET name = :newName, imageUrl = :newImageUrl WHERE name = :oldName")
    suspend fun updateProductDetails(oldName: String, newName: String, newImageUrl: String?)
}
