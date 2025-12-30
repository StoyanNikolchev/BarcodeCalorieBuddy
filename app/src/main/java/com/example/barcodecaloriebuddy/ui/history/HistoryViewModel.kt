package com.example.barcodecaloriebuddy.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.barcodecaloriebuddy.data.FoodItem
import com.example.barcodecaloriebuddy.data.FoodRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class HistoryState(
    val itemsByDate: Map<LocalDate, List<FoodItem>> = emptyMap()
)

class HistoryViewModel(private val foodRepository: FoodRepository) : ViewModel() {

    val historyState: StateFlow<HistoryState> = foodRepository.getAllFoodItems()
        .map { items ->
            val today = LocalDate.now(ZoneId.systemDefault())
            val pastItems = items.filter {
                Instant.ofEpochMilli(it.date).atZone(ZoneId.systemDefault()).toLocalDate() != today
            }
            val groupedItems = pastItems.groupBy {
                Instant.ofEpochMilli(it.date).atZone(ZoneId.systemDefault()).toLocalDate()
            }
            HistoryState(itemsByDate = groupedItems)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = HistoryState()
        )

    fun addHistoryItemToTodaysLog(foodItem: FoodItem, quantity: Int) {
        viewModelScope.launch {
            val caloriesPer100g = foodItem.caloriesPer100g ?: 0
            val totalCalories = (caloriesPer100g / 100.0 * quantity).toInt()
            // Create a new item for today
            val newItem = foodItem.copy(
                id = 0, 
                calories = totalCalories,
                quantity = quantity,
                date = System.currentTimeMillis(),
                isArchived = false
            )
            foodRepository.addOrUpdateFoodItem(newItem)
        }
    }
}
