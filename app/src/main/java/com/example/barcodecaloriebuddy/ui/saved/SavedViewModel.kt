package com.example.barcodecaloriebuddy.ui.saved

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

class SavedViewModel(private val foodRepository: FoodRepository) : ViewModel() {

    val savedFoodItems: StateFlow<List<FoodItem>> = foodRepository.getAllFoodItems()
        .map { items -> items.distinctBy { it.name } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun updateSavedItem(context: Context, item: FoodItem, newName: String, newImageUri: String?) {
        viewModelScope.launch {
            val permanentImageUri = newImageUri?.let { saveImageLocally(context, Uri.parse(it)) }
            foodRepository.updateProductDetails(item.name, newName, permanentImageUri?.toString() ?: item.imageUrl)
        }
    }

    fun addSavedItemToTodaysLog(foodItem: FoodItem, quantity: Int) {
        viewModelScope.launch {
            val caloriesPer100g = foodItem.caloriesPer100g ?: 0
            val totalCalories = (caloriesPer100g / 100.0 * quantity).toInt()
            val newItem = foodItem.copy(
                id = 0, // Let Room auto-generate a new ID
                calories = totalCalories,
                quantity = quantity,
                date = System.currentTimeMillis(),
                isArchived = false
            )
            foodRepository.addOrUpdateFoodItem(newItem)
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
}
