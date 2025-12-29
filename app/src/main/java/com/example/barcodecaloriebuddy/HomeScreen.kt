package com.example.barcodecaloriebuddy

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
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
import com.example.barcodecaloriebuddy.ui.home.HomeScreenViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(modifier: Modifier = Modifier, onNavigateToScanner: () -> Unit) {
    val viewModel: HomeScreenViewModel = viewModel(
        factory = ViewModelFactory(Injection.provideFoodRepository(LocalContext.current))
    )
    val todaysFoodItems by viewModel.todaysFoodItems.collectAsState()
    val todaysCalories by viewModel.todaysCalories.collectAsState()
    var showAddFoodDialog by remember { mutableStateOf(false) }
    var showFabMenu by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            if (todaysFoodItems.isNotEmpty()) {
                CenterAlignedTopAppBar(
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Today's Calories", style = MaterialTheme.typography.titleLarge)
                            Text(todaysCalories.toString(), style = MaterialTheme.typography.headlineLarge)
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            Box {
                FloatingActionButton(onClick = { showFabMenu = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add food")
                }
                DropdownMenu(
                    expanded = showFabMenu,
                    onDismissRequest = { showFabMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Add Manually", style = MaterialTheme.typography.bodyLarge) },
                        onClick = {
                            showAddFoodDialog = true
                            showFabMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Scan Barcode", style = MaterialTheme.typography.bodyLarge) },
                        onClick = {
                            onNavigateToScanner()
                            showFabMenu = false
                        }
                    )
                }
            }
        }
    ) { paddingValues ->
        if (todaysFoodItems.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Today's Calories", style = MaterialTheme.typography.titleLarge)
                    Text(todaysCalories.toString(), style = MaterialTheme.typography.headlineLarge)
                }
            }
        } else {
            LazyColumn(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
                items(todaysFoodItems, key = { it.id }) { foodItem ->
                    FoodItemRow(
                        foodItem = foodItem, 
                        onDelete = { viewModel.deleteFoodItem(it) },
                        onToggleFavorite = { viewModel.toggleFavorite(it) }
                    )
                    Divider()
                }
            }
        }
    }

    if (showAddFoodDialog) {
        AddFoodDialog(
            onDismiss = { showAddFoodDialog = false },
            onConfirm = { name, caloriesPer100g, quantity ->
                viewModel.addFoodItem(name, caloriesPer100g, quantity)
                showAddFoodDialog = false
            }
        )
    }
}

@Composable
private fun FoodItemRow(foodItem: FoodItem, onDelete: (FoodItem) -> Unit, onToggleFavorite: (FoodItem) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
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
            val title = if (foodItem.caloriesPer100g != null) {
                "${foodItem.name} (${foodItem.caloriesPer100g} kcal/100g)"
            } else {
                foodItem.name
            }
            Text(text = title)
            
            val subtitle = if (foodItem.quantity > 0) {
                "${foodItem.quantity}g - ${foodItem.calories} kcal"
            } else {
                "${foodItem.calories} kcal"
            }
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall)
        }
        IconButton(onClick = { onToggleFavorite(foodItem) }) {
            Icon(
                imageVector = if (foodItem.isFavorite) Icons.Filled.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = "Mark as favorite"
            )
        }
        IconButton(onClick = { onDelete(foodItem) }) {
            Icon(Icons.Default.Delete, contentDescription = "Delete food")
        }
    }
}

@Composable
private fun AddFoodDialog(onDismiss: () -> Unit, onConfirm: (String, Int, Int) -> Unit) {
    var name by remember { mutableStateOf("") }
    var caloriesPer100g by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Food Manually") },
        text = {
            Column {
                TextField(
                    value = name, 
                    onValueChange = { name = it }, 
                    label = { Text("Food Name") },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = caloriesPer100g,
                    onValueChange = { caloriesPer100g = it },
                    label = { Text("Calories per 100g") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = { Text("Quantity (grams)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val nameIsValid = name.isNotBlank()
                val caloriesInt = caloriesPer100g.toIntOrNull()
                val quantityInt = quantity.toIntOrNull()
                if (nameIsValid && caloriesInt != null && quantityInt != null) {
                    onConfirm(name, caloriesInt, quantityInt)
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
