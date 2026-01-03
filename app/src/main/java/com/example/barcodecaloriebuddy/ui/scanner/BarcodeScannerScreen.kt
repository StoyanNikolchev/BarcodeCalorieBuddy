package com.example.barcodecaloriebuddy.ui.scanner

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.barcodecaloriebuddy.data.FoodItem
import com.example.barcodecaloriebuddy.di.Injection
import com.example.barcodecaloriebuddy.network.FoodFactsApiService
import com.example.barcodecaloriebuddy.network.dto.Product
import com.example.barcodecaloriebuddy.network.dto.ProductResponse
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonPrimitive
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarcodeScannerScreen(modifier: Modifier = Modifier, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val foodRepository = remember { Injection.provideFoodRepository(context) }

    var hasCameraPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasCameraPermission = granted }
    )

    var uiState by remember { mutableStateOf<ScannerUiState>(ScannerUiState.Scanning) }

    fun resetScannerState() {
        uiState = ScannerUiState.Scanning
    }

    LaunchedEffect(key1 = true) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Scan Barcode") },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            when (val state = uiState) {
                is ScannerUiState.Scanning -> {
                    CameraPreview { barcode ->
                        uiState = ScannerUiState.Loading
                        coroutineScope.launch {
                            val existingItem = foodRepository.findMostRecentByBarcode(barcode)
                            if (existingItem != null) {
                                uiState = ScannerUiState.ProductFound(existingItem.toProductResponse(), isFromCache = true)
                            } else {
                                try {
                                    val apiService = FoodFactsApiService.create()
                                    val response = apiService.getProduct(barcode)
                                    if (response.status == 1 && response.product != null) {
                                        uiState = ScannerUiState.ProductFound(response, isFromCache = false)
                                    } else {
                                        uiState = ScannerUiState.ProductNotFound
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    uiState = ScannerUiState.Error("Network request failed.")
                                }
                            }
                        }
                    }
                }
                is ScannerUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is ScannerUiState.ProductFound -> {
                    ProductDetails(state) { updatedProduct ->
                        coroutineScope.launch {
                            foodRepository.addOrUpdateFoodItem(updatedProduct)
                            onDismiss()
                        }
                    }
                }
                is ScannerUiState.ProductNotFound -> {
                    ErrorScreen("Product not found for this barcode.", onRetry = { resetScannerState() })
                }
                is ScannerUiState.Error -> {
                    ErrorScreen(state.message, onRetry = { resetScannerState() })
                }
            }

            if (!hasCameraPermission && uiState is ScannerUiState.Scanning) {
                Text("Camera permission not granted", modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

sealed class ScannerUiState {
    object Scanning : ScannerUiState()
    object Loading : ScannerUiState()
    data class ProductFound(val response: ProductResponse, val isFromCache: Boolean) : ScannerUiState()
    object ProductNotFound : ScannerUiState()
    data class Error(val message: String) : ScannerUiState()
}

@Composable
fun ErrorScreen(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(message)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text("Scan Again")
        }
    }
}

@Composable
private fun CameraPreview(onBarcodeScanned: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var isScanning by remember { mutableStateOf(true) }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val executor = Executors.newSingleThreadExecutor()
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                imageAnalysis.setAnalyzer(executor) { imageProxy ->
                    if (isScanning) {
                        processImageProxy(imageProxy) { barcode ->
                            isScanning = false
                            onBarcodeScanned(barcode)
                        }
                    }
                }

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageAnalysis)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        },
        modifier = Modifier.fillMaxSize(),
        onRelease = {
            cameraProviderFuture.get().unbindAll()
        }
    )
}

@Composable
private fun ProductDetails(
    state: ScannerUiState.ProductFound,
    onAdd: (FoodItem) -> Unit
) {
    val product = state.response.product
    var quantity by remember { mutableStateOf("") }
    var caloriesPer100g by remember(state.response.code) {
        val initialCalories = product?.nutriments?.energyKcal100g.toIntOrNull()?.toString() ?: "0"
        mutableStateOf(initialCalories)
    }
    var productName by remember(state.response.code) {
        mutableStateOf(product?.productName ?: "Product name not found")
    }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var showEditCaloriesDialog by remember { mutableStateOf(false) }
    var showEditNameDialog by remember { mutableStateOf(false) }
    var showImageSourceDialog by remember { mutableStateOf(false) }
    
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        imageUri = uri
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        val displayImage: Any? = imageUri ?: product?.imageUrl
        Box(modifier = Modifier.size(150.dp).clickable { showImageSourceDialog = true }) {
            if (displayImage != null) {
                AsyncImage(
                    model = displayImage,
                    contentDescription = productName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.AddAPhoto,
                    contentDescription = "Add a photo",
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            val icon = if (state.isFromCache) Icons.Default.CheckCircle else Icons.Default.Error
            val tint = if (state.isFromCache) Color.Green else MaterialTheme.colorScheme.error
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(if (state.isFromCache) "In your database" else "New item", style = MaterialTheme.typography.bodySmall)
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = productName)
            if (!state.isFromCache) {
                IconButton(onClick = { showEditNameDialog = true }) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Name")
                }
            }
        }

        val isCaloriesMissing = caloriesPer100g == "0"
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Calories per 100g: $caloriesPer100g",
                color = if (isCaloriesMissing && !state.isFromCache) MaterialTheme.colorScheme.error else Color.Unspecified
            )
            if (!state.isFromCache) {
                IconButton(onClick = { showEditCaloriesDialog = true }) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Calories")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = quantity,
            onValueChange = { quantity = it },
            label = { Text("Quantity (grams)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        val isAddEnabled = (quantity.toIntOrNull() ?: 0) > 0 && (caloriesPer100g.toIntOrNull() ?: 0) > 0
        Button(
            onClick = {
                val finalCaloriesPer100g = caloriesPer100g.toIntOrNull() ?: 0
                val grams = quantity.toIntOrNull() ?: 0
                val totalCalories = (finalCaloriesPer100g / 100.0 * grams).toInt()
                val itemToAdd = FoodItem(
                    name = productName,
                    calories = totalCalories,
                    quantity = grams,
                    imageUrl = imageUri?.toString() ?: product?.imageUrl,
                    caloriesPer100g = finalCaloriesPer100g,
                    barcode = state.response.code
                )
                onAdd(itemToAdd)
            },
            enabled = isAddEnabled
        ) {
            Text("ADD")
        }
    }

    if (showEditCaloriesDialog) {
        EditCaloriesDialog(
            initialValue = caloriesPer100g,
            onDismiss = { showEditCaloriesDialog = false },
            onConfirm = {
                caloriesPer100g = it
                showEditCaloriesDialog = false
            }
        )
    }

    if (showEditNameDialog) {
        EditNameDialog(
            initialValue = if (productName == "Product name not found") "" else productName,
            onDismiss = { showEditNameDialog = false },
            onConfirm = { 
                productName = it
                showEditNameDialog = false
            }
        )
    }
    
    if (showImageSourceDialog) {
        ChooseImageSourceDialog(
            onDismiss = { showImageSourceDialog = false },
            onTakePhoto = { /* TODO */ },
            onChooseFromGallery = { 
                galleryLauncher.launch("image/*")
                showImageSourceDialog = false
            }
        )
    }
}

@Composable
private fun EditCaloriesDialog(
    initialValue: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var calories by remember { mutableStateOf(initialValue) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Calories") },
        text = {
            TextField(
                value = calories,
                onValueChange = { calories = it },
                label = { Text("Calories per 100g") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )
        },
        confirmButton = {
            Button(onClick = { onConfirm(calories) }) {
                Text("Confirm")
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
private fun EditNameDialog(
    initialValue: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf(initialValue) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Product Name") },
        text = {
            TextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Product Name") },
                singleLine = true
            )
        },
        confirmButton = {
            Button(onClick = { onConfirm(name) }) {
                Text("Confirm")
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


@androidx.annotation.OptIn(ExperimentalGetImage::class)
private fun processImageProxy(imageProxy: ImageProxy, onBarcodeScanned: (String) -> Unit) {
    val image = imageProxy.image ?: return
    val inputImage = InputImage.fromMediaImage(image, imageProxy.imageInfo.rotationDegrees)
    val scanner = BarcodeScanning.getClient(BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS).build())

    scanner.process(inputImage)
        .addOnSuccessListener { barcodes ->
            barcodes.firstOrNull()?.rawValue?.let(onBarcodeScanned)
        }
        .addOnCompleteListener {
            imageProxy.close()
        }
}

fun JsonElement?.toIntOrNull(): Int? {
    return this?.jsonPrimitive?.content?.toDoubleOrNull()?.toInt()
}

private fun FoodItem.toProductResponse(): ProductResponse {
    return ProductResponse(
        code = this.barcode,
        product = Product(
            productName = this.name,
            imageUrl = this.imageUrl,
            nutriments = com.example.barcodecaloriebuddy.network.dto.Nutriments(
                energyKcal100g = this.caloriesPer100g?.let { Json.Default.encodeToJsonElement(it) }
            )
        ),
        status = 1
    )
}
