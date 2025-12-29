package com.example.barcodecaloriebuddy.ui.scanner

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.example.barcodecaloriebuddy.network.dto.ProductResponse
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonPrimitive
import java.util.concurrent.Executors

@Composable
fun BarcodeScannerScreen(modifier: Modifier = Modifier) {
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
    var quantity by remember { mutableStateOf("") }

    fun resetScannerState() {
        uiState = ScannerUiState.Scanning
        quantity = ""
    }

    LaunchedEffect(key1 = true) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        when (val state = uiState) {
            is ScannerUiState.Scanning -> {
                CameraPreview { barcode ->
                    uiState = ScannerUiState.Loading
                    coroutineScope.launch {
                        try {
                            val apiService = FoodFactsApiService.create()
                            val response = apiService.getProduct(barcode)
                            if (response.status == 1 && response.product != null) {
                                uiState = ScannerUiState.ProductFound(response)
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
            is ScannerUiState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            is ScannerUiState.ProductFound -> {
                ProductDetails(state.response, quantity, onQuantityChange = { quantity = it }) {
                    coroutineScope.launch {
                        val product = state.response.product
                        val caloriesPer100g = product?.nutriments?.energyKcal100g.toIntOrNull() ?: 0
                        val grams = quantity.toIntOrNull() ?: 0
                        val totalCalories = (caloriesPer100g / 100.0 * grams).toInt()
                        foodRepository.insertFoodItem(
                            FoodItem(
                                name = product?.productName ?: "Unknown Product",
                                calories = totalCalories,
                                quantity = grams
                            )
                        )
                        resetScannerState()
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

sealed class ScannerUiState {
    object Scanning : ScannerUiState()
    object Loading : ScannerUiState()
    data class ProductFound(val response: ProductResponse) : ScannerUiState()
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
    productResponse: ProductResponse?,
    quantity: String,
    onQuantityChange: (String) -> Unit,
    onAdd: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val product = productResponse?.product
        val productName = product?.productName ?: "Product name not found"
        val caloriesPer100g = product?.nutriments?.energyKcal100g.toIntOrNull() ?: 0

        AsyncImage(
            model = product?.imageUrl,
            contentDescription = productName,
            modifier = Modifier.size(150.dp),
            contentScale = ContentScale.Fit
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = productName)
        Text(text = "Calories per 100g: $caloriesPer100g")

        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = quantity,
            onValueChange = onQuantityChange,
            label = { Text("Quantity (grams)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onAdd) {
            Text("ADD")
        }
    }
}

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
