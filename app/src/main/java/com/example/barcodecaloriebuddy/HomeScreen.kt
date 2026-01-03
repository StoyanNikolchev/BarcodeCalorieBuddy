package com.example.barcodecaloriebuddy

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.barcodecaloriebuddy.data.FoodItem
import com.example.barcodecaloriebuddy.di.Injection
import com.example.barcodecaloriebuddy.ui.ViewModelFactory
import com.example.barcodecaloriebuddy.ui.home.HomeScreenViewModel
import java.io.File

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
    var itemToEdit by remember { mutableStateOf<FoodItem?>(null) }
    val context = LocalContext.current

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
                        text = { Text("Add Manually") },
                        onClick = {
                            showAddFoodDialog = true
                            showFabMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Scan Barcode") },
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
                        onToggleFavorite = { viewModel.toggleFavorite(it) },
                        onClick = { itemToEdit = foodItem }
                    )
                    Divider()
                }
            }
        }
    }

    if (showAddFoodDialog) {
        AddFoodDialog(
            onDismiss = { showAddFoodDialog = false },
            onConfirm = { name, caloriesPer100g, quantity, imageUri ->
                viewModel.addFoodItem(context, name, caloriesPer100g, quantity, imageUri?.toString())
                showAddFoodDialog = false
            }
        )
    }

    itemToEdit?.let {
        EditTodaysItemDialog(
            foodItem = it,
            onDismiss = { itemToEdit = null },
            onConfirm = { updatedFoodItem ->
                viewModel.updateFoodItem(updatedFoodItem)
                itemToEdit = null
            }
        )
    }
}

@Composable
private fun FoodItemRow(
    foodItem: FoodItem, 
    onDelete: (FoodItem) -> Unit, 
    onToggleFavorite: (FoodItem) -> Unit,
    onClick: () -> Unit
) {
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
            val subtitle = if (foodItem.quantity > 0) {
                "${foodItem.quantity}g - ${foodItem.calories} kcal"
            } else {
                "${foodItem.calories} kcal"
            }
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall)
            foodItem.caloriesPer100g?.let {
                Text(text = "($it kcal/100g)", style = MaterialTheme.typography.bodySmall)
            }
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
private fun AddFoodDialog(onDismiss: () -> Unit, onConfirm: (String, Int, Int, Uri?) -> Unit) {
    var name by remember { mutableStateOf("") }
    var caloriesPer100g by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var showImageSourceDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        imageUri = uri
    }
    
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            imageUri = tempCameraUri
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Food Manually") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(modifier = Modifier.size(128.dp).clickable { showImageSourceDialog = true }) {
                    if (imageUri != null) {
                        AsyncImage(
                            model = imageUri,
                            contentDescription = "Selected image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add image",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
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
                    onConfirm(name, caloriesInt, quantityInt, imageUri)
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

    if (showImageSourceDialog) {
        ChooseImageSourceDialog(
            onDismiss = { showImageSourceDialog = false },
            onTakePhoto = { 
                val uri = ComposeFileProvider.getImageUri(context)
                tempCameraUri = uri
                cameraLauncher.launch(uri)
                showImageSourceDialog = false
            },
            onChooseFromGallery = { 
                galleryLauncher.launch("image/*")
                showImageSourceDialog = false
            }
        )
    }
}

@Composable
private fun ChooseImageSourceDialog(
    onDismiss: () -> Unit,
    onTakePhoto: () -> Unit,
    onChooseFromGallery: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change Image") },
        text = { Text("How would you like to set the image?") },
        confirmButton = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Button(onClick = onTakePhoto, modifier = Modifier.fillMaxWidth()) {
                    Text("Take Photo")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onChooseFromGallery, modifier = Modifier.fillMaxWidth()) {
                    Text("Choose from Gallery")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun EditTodaysItemDialog(
    foodItem: FoodItem,
    onDismiss: () -> Unit,
    onConfirm: (FoodItem) -> Unit
) {
    var calories by remember { mutableStateOf(foodItem.calories.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Calories") },
        text = {
            TextField(
                value = calories,
                onValueChange = { calories = it },
                label = { Text("Total Calories") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )
        },
        confirmButton = {
            Button(onClick = {
                val caloriesInt = calories.toIntOrNull() ?: 0
                if (caloriesInt > 0) {
                    onConfirm(foodItem.copy(calories = caloriesInt))
                }
            }) {
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

class ComposeFileProvider : FileProvider(
    com.example.barcodecaloriebuddy.R.xml.file_paths
) {
    companion object {
        fun getImageUri(context: Context): Uri {
            val directory = File(context.cacheDir, "images")
            directory.mkdirs()
            val file = File.createTempFile(
                "selected_image_",
                ".jpg",
                directory,
            )
            val authority = context.packageName + ".fileprovider"
            return getUriForFile(
                context,
                authority,
                file,
            )
        }
    }
}
