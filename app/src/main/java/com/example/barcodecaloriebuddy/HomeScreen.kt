package com.example.barcodecaloriebuddy

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.barcodecaloriebuddy.di.Injection
import com.example.barcodecaloriebuddy.ui.ViewModelFactory
import com.example.barcodecaloriebuddy.ui.home.HomeScreenViewModel

@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    val viewModel: HomeScreenViewModel = viewModel(
        factory = ViewModelFactory(Injection.provideFoodRepository(LocalContext.current))
    )
    val todaysCalories by viewModel.todaysCalories.collectAsState()

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("Total calories for today: $todaysCalories")
    }
}
