package com.example.barcodecaloriebuddy.ui.saved

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.barcodecaloriebuddy.data.FoodItem
import com.example.barcodecaloriebuddy.data.FoodRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SavedViewModel(private val foodRepository: FoodRepository) : ViewModel() {

    val savedFoodItems: StateFlow<List<FoodItem>> = foodRepository.getAllFoodItems()
        .map { items -> items.distinctBy { it.name } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun updateItemName(foodItem: FoodItem, newName: String) {
        viewModelScope.launch {
            foodRepository.updateFoodItem(foodItem.copy(name = newName))
        }
    }
}
