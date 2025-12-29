package com.example.barcodecaloriebuddy.ui.favorites

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.barcodecaloriebuddy.data.FoodItem
import com.example.barcodecaloriebuddy.di.Injection
import com.example.barcodecaloriebuddy.ui.ViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(modifier: Modifier = Modifier) {
    val viewModel: FavoritesViewModel = viewModel(
        factory = ViewModelFactory(Injection.provideFoodRepository(LocalContext.current))
    )
    val favoriteFoodItems by viewModel.favoriteFoodItems.collectAsState()
    var selectedFoodItem by remember { mutableStateOf<FoodItem?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(title = { Text("Favorites") })
        }
    ) { paddingValues ->
        LazyColumn(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            items(favoriteFoodItems, key = { it.id }) { foodItem ->
                FavoriteItemRow(
                    foodItem = foodItem, 
                    onClick = { selectedFoodItem = foodItem },
                    onUnfavorite = { viewModel.unfavoriteItem(foodItem) }
                )
                Divider()
            }
        }
    }

    selectedFoodItem?.let { foodItem ->
        AddFavoriteToLogDialog(
            foodItem = foodItem,
            onDismiss = { selectedFoodItem = null },
            onConfirm = { quantity ->
                viewModel.addFavoriteToTodaysLog(foodItem, quantity)
                selectedFoodItem = null
            }
        )
    }
}

@Composable
private fun FavoriteItemRow(foodItem: FoodItem, onClick: () -> Unit, onUnfavorite: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (foodItem.imageUrl.isNullOrEmpty()) {
            Icon(
                imageVector = Icons.Filled.Restaurant,
                contentDescription = foodItem.name,
                modifier = Modifier.size(64.dp)
            )
        } else {
            AsyncImage(
                model = foodItem.imageUrl,
                contentDescription = foodItem.name,
                modifier = Modifier.size(64.dp),
                contentScale = ContentScale.Crop
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = foodItem.name)
            foodItem.caloriesPer100g?.let {
                Text(text = "$it kcal/100g", style = MaterialTheme.typography.bodySmall)
            }
        }
        IconButton(onClick = onUnfavorite) {
            Icon(Icons.Filled.Favorite, contentDescription = "Unfavorite", tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun AddFavoriteToLogDialog(
    foodItem: FoodItem,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var quantity by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add ${foodItem.name} to today's log?") },
        text = {
            TextField(
                value = quantity,
                onValueChange = { quantity = it },
                label = { Text("Quantity (grams)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        },
        confirmButton = {
            Button(onClick = {
                val quantityInt = quantity.toIntOrNull() ?: 0
                if (quantityInt > 0) {
                    onConfirm(quantityInt)
                }
            }) {
                Text("Add")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
