package com.example.barcodecaloriebuddy.ui.saved

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
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
fun SavedScreen(modifier: Modifier = Modifier) {
    val viewModel: SavedViewModel = viewModel(
        factory = ViewModelFactory(Injection.provideFoodRepository(LocalContext.current))
    )
    val savedFoodItems by viewModel.savedFoodItems.collectAsState()
    var foodItemToEdit by remember { mutableStateOf<FoodItem?>(null) }
    var foodItemToAdd by remember { mutableStateOf<FoodItem?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { CenterAlignedTopAppBar(title = { Text("Saved Items") }) }
    ) { paddingValues ->
        LazyColumn(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            items(savedFoodItems, key = { it.id }) { foodItem ->
                SavedItemRow(
                    foodItem = foodItem, 
                    onEdit = { foodItemToEdit = foodItem },
                    onClick = { foodItemToAdd = foodItem }
                )
                Divider()
            }
        }
    }

    foodItemToEdit?.let { foodItem ->
        EditNameDialog(
            foodItem = foodItem,
            onDismiss = { foodItemToEdit = null },
            onConfirm = { newName ->
                viewModel.updateItemName(foodItem, newName)
                foodItemToEdit = null
            }
        )
    }
    
    foodItemToAdd?.let { foodItem ->
        AddSavedItemToLogDialog(
            foodItem = foodItem,
            onDismiss = { foodItemToAdd = null },
            onConfirm = { quantity ->
                viewModel.addSavedItemToTodaysLog(foodItem, quantity)
                foodItemToAdd = null
            }
        )
    }
}

@Composable
private fun SavedItemRow(foodItem: FoodItem, onEdit: () -> Unit, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
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
        IconButton(onClick = onEdit) {
            Icon(Icons.Default.Edit, contentDescription = "Edit Name")
        }
    }
}

@Composable
private fun EditNameDialog(
    foodItem: FoodItem,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf(foodItem.name) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Item Name") },
        text = {
            TextField(value = name, onValueChange = { name = it }, singleLine = true)
        },
        confirmButton = {
            Button(onClick = { onConfirm(name) }) {
                Text("Save")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun AddSavedItemToLogDialog(
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
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
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
