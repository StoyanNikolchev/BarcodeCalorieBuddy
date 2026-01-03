package com.example.barcodecaloriebuddy.ui.home

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.barcodecaloriebuddy.data.FoodItem
import com.example.barcodecaloriebuddy.data.FoodRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Calendar

class HomeScreenViewModel(private val foodRepository: FoodRepository) : ViewModel() {

    val todaysFoodItems: StateFlow<List<FoodItem>> = foodRepository.getTodaysFoodItems(
        getStartOfDayInMillis(),
        getEndOfDayInMillis()
    ).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val todaysCalories: StateFlow<Int> = todaysFoodItems.map { foodItems ->
        foodItems.sumOf { it.calories }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    fun addFoodItem(context: Context, name: String, caloriesPer100g: Int, quantity: Int, imageUrl: String?) {
        viewModelScope.launch {
            val permanentImageUri = imageUrl?.let { saveImageLocally(context, Uri.parse(it)) }
            val totalCalories = (caloriesPer100g / 100.0 * quantity).toInt()
            foodRepository.addOrUpdateFoodItem(
                FoodItem(
                    name = name, 
                    calories = totalCalories, 
                    quantity = quantity, 
                    caloriesPer100g = caloriesPer100g,
                    imageUrl = permanentImageUri?.toString()
                )
            )
        }
    }

    fun updateFoodItem(foodItem: FoodItem) {
        viewModelScope.launch {
            foodRepository.updateFoodItem(foodItem)
        }
    }

    fun deleteFoodItem(foodItem: FoodItem) {
        viewModelScope.launch {
            val hasOtherEntries = foodRepository.countOtherEntries(foodItem.name, foodItem.id) > 0
            if (foodItem.isFavorite || hasOtherEntries) {
                // Soft delete: just archive it so it doesn't show up today
                foodRepository.updateFoodItem(foodItem.copy(isArchived = true))
            } else {
                // Hard delete: not a favorite and no other history, so remove it completely
                foodRepository.deleteFoodItem(foodItem)
            }
        }
    }

    fun toggleFavorite(foodItem: FoodItem) {
        viewModelScope.launch {
            foodRepository.updateFoodItem(foodItem.copy(isFavorite = !foodItem.isFavorite))
        }
    }
    
    private suspend fun saveImageLocally(context: Context, uri: Uri): Uri? = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val imagesDir = File(context.filesDir, "images")
            if (!imagesDir.exists()) imagesDir.mkdirs()
            val outputFile = File.createTempFile("item_", ".jpg", imagesDir)
            val outputStream = FileOutputStream(outputFile)
            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()
            Uri.fromFile(outputFile)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
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
