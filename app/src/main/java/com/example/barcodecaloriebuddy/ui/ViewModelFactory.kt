package com.example.barcodecaloriebuddy.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.barcodecaloriebuddy.data.FoodRepository
import com.example.barcodecaloriebuddy.ui.favorites.FavoritesViewModel
import com.example.barcodecaloriebuddy.ui.history.HistoryViewModel
import com.example.barcodecaloriebuddy.ui.home.HomeScreenViewModel
import com.example.barcodecaloriebuddy.ui.saved.SavedViewModel

class ViewModelFactory(private val foodRepository: FoodRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeScreenViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeScreenViewModel(foodRepository) as T
        } else if (modelClass.isAssignableFrom(FavoritesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FavoritesViewModel(foodRepository) as T
        } else if (modelClass.isAssignableFrom(HistoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HistoryViewModel(foodRepository) as T
        } else if (modelClass.isAssignableFrom(SavedViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SavedViewModel(foodRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
