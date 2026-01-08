package com.example.barcodecaloriebuddy.ui.favorites

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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
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
import com.example.barcodecaloriebuddy.ui.saved.ComposeFileProvider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val viewModel: FavoritesViewModel = viewModel(
        factory = ViewModelFactory(Injection.provideFoodRepository(context))
    )
    val favoriteFoodItems by viewModel.favoriteFoodItems.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    var foodItemToEdit by remember { mutableStateOf<FoodItem?>(null) }
    var foodItemToAdd by remember { mutableStateOf<FoodItem?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            Column {
                CenterAlignedTopAppBar(title = { Text("Favorites") })
                TextField(
                    value = searchQuery,
                    onValueChange = { viewModel.onSearchQueryChange(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("Search favorites...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        disabledContainerColor = MaterialTheme.colorScheme.surface,
                    )
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            items(favoriteFoodItems, key = { it.id }) { foodItem ->
                FavoriteItemRow(
                    foodItem = foodItem,
                    onClick = { foodItemToAdd = foodItem },
                    onUnfavorite = { viewModel.unfavoriteItem(foodItem) },
                    onEdit = { foodItemToEdit = foodItem }
                )
                Divider()
            }
        }
    }

    foodItemToEdit?.let { item ->
        EditFavoriteItemDialog(
            foodItem = item,
            onDismiss = { foodItemToEdit = null },
            onConfirm = { newName, newImageUri ->
                viewModel.updateFavoriteItem(context, item, newName, newImageUri?.toString())
                foodItemToEdit = null
            }
        )
    }

    foodItemToAdd?.let { foodItem ->
        AddFavoriteToLogDialog(
            foodItem = foodItem,
            onDismiss = { foodItemToAdd = null },
            onConfirm = { quantity ->
                viewModel.addFavoriteToTodaysLog(foodItem, quantity)
                foodItemToAdd = null
            }
        )
    }
}

@Composable
private fun FavoriteItemRow(
    foodItem: FoodItem,
    onClick: () -> Unit,
    onUnfavorite: () -> Unit,
    onEdit: () -> Unit
) {
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
        IconButton(onClick = onEdit) {
            Icon(Icons.Default.Edit, contentDescription = "Edit")
        }
    }
}

@Composable
private fun EditFavoriteItemDialog(
    foodItem: FoodItem,
    onDismiss: () -> Unit,
    onConfirm: (String, Uri?) -> Unit
) {
    var name by remember { mutableStateOf(foodItem.name) }
    var imageUri by remember { mutableStateOf<Uri?>(foodItem.imageUrl?.let { Uri.parse(it) }) }
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
        title = { Text("Edit Item") },
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
                TextField(value = name, onValueChange = { name = it }, singleLine = true)
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(name, imageUri) }) {
                Text("Save")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    if (showImageSourceDialog) {
        com.example.barcodecaloriebuddy.ui.saved.ChooseImageSourceDialog(
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
