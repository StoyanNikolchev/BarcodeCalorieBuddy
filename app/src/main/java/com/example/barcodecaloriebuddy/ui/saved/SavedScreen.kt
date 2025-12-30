package com.example.barcodecaloriebuddy.ui.saved

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { CenterAlignedTopAppBar(title = { Text("Saved Items") }) }
    ) { paddingValues ->
        LazyColumn(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            items(savedFoodItems, key = { it.id }) { foodItem ->
                SavedItemRow(foodItem = foodItem, onEdit = { foodItemToEdit = foodItem })
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
}

@Composable
private fun SavedItemRow(foodItem: FoodItem, onEdit: () -> Unit) {
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
        Text(text = foodItem.name, modifier = Modifier.weight(1f))
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
