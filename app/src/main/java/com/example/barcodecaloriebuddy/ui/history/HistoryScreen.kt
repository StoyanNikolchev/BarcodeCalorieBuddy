package com.example.barcodecaloriebuddy.ui.history

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
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
import com.example.barcodecaloriebuddy.ui.components.ImagePreviewDialog
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(modifier: Modifier = Modifier) {
    val viewModel: HistoryViewModel = viewModel(
        factory = ViewModelFactory(Injection.provideFoodRepository(LocalContext.current))
    )
    val historyState by viewModel.historyState.collectAsState()
    var selectedFoodItem by remember { mutableStateOf<FoodItem?>(null) }
    var previewImageUrl by remember { mutableStateOf<String?>(null) }
    var showImagePreview by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { CenterAlignedTopAppBar(title = { Text("History") }) }
    ) { paddingValues ->
        LazyColumn(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            val sortedDates = historyState.itemsByDate.keys.sortedDescending()
            sortedDates.forEach { date ->
                val items = historyState.itemsByDate[date] ?: emptyList()
                item {
                    var isExpanded by remember { mutableStateOf(false) }
                    Column {
                        DaySummaryRow(
                            date = date,
                            totalCalories = items.sumOf { it.calories },
                            isExpanded = isExpanded,
                            onToggleExpand = { isExpanded = !isExpanded }
                        )
                        AnimatedVisibility(visible = isExpanded) {
                            Column {
                                items.forEach { foodItem ->
                                    FoodItemHistoryRow(
                                        foodItem = foodItem, 
                                        onClick = { selectedFoodItem = foodItem },
                                        onImageClick = {
                                            previewImageUrl = foodItem.imageUrl
                                            showImagePreview = true
                                        }
                                    )
                                    Divider()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    selectedFoodItem?.let {
        AddHistoryItemToLogDialog(
            foodItem = it,
            onDismiss = { selectedFoodItem = null },
            onConfirm = { quantity ->
                viewModel.addHistoryItemToTodaysLog(it, quantity)
                selectedFoodItem = null
            }
        )
    }

    if (showImagePreview) {
        ImagePreviewDialog(
            imageUrl = previewImageUrl,
            onDismiss = { showImagePreview = false }
        )
    }
}

@Composable
private fun DaySummaryRow(
    date: LocalDate,
    totalCalories: Int,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggleExpand() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        val formattedDate = date.format(DateTimeFormatter.ofPattern("MMMM dd, yyyy"))
        Text(text = formattedDate, style = MaterialTheme.typography.titleMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "$totalCalories kcal", style = MaterialTheme.typography.bodyLarge)
            Icon(
                imageVector = if (isExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                contentDescription = if (isExpanded) "Collapse" else "Expand"
            )
        }
    }
}

@Composable
private fun FoodItemHistoryRow(
    foodItem: FoodItem, 
    onClick: () -> Unit,
    onImageClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(start = 32.dp, top = 8.dp, bottom = 8.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (foodItem.imageUrl.isNullOrEmpty()) {
            Icon(
                imageVector = Icons.Default.Restaurant,
                contentDescription = foodItem.name,
                modifier = Modifier
                    .size(48.dp)
                    .clickable(onClick = onImageClick)
            )
        } else {
            AsyncImage(
                model = foodItem.imageUrl,
                contentDescription = foodItem.name,
                modifier = Modifier
                    .size(48.dp)
                    .clickable(onClick = onImageClick),
                contentScale = ContentScale.Crop
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = foodItem.name)
            Text(text = "${foodItem.quantity}g - ${foodItem.calories} kcal", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun AddHistoryItemToLogDialog(
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
