package com.example.barcodecaloriebuddy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import com.example.barcodecaloriebuddy.ui.favorites.FavoritesScreen
import com.example.barcodecaloriebuddy.ui.history.HistoryScreen
import com.example.barcodecaloriebuddy.ui.saved.SavedScreen
import com.example.barcodecaloriebuddy.ui.scanner.BarcodeScannerScreen
import com.example.barcodecaloriebuddy.ui.theme.BarcodeCalorieBuddyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BarcodeCalorieBuddyTheme {
                BarcodeCalorieBuddyApp()
            }
        }
    }
}

@PreviewScreenSizes
@Composable
fun BarcodeCalorieBuddyApp() {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }
    var showScanner by remember { mutableStateOf(false) }

    if (showScanner) {
        BarcodeScannerScreen(onDismiss = { showScanner = false })
    } else {
        NavigationSuiteScaffold(
            navigationSuiteItems = {
                AppDestinations.entries.forEach {                    item(
                        icon = {
                            Icon(
                                it.icon,
                                contentDescription = it.label
                            )
                        },
                        label = { Text(it.label) },
                        selected = it == currentDestination,
                        onClick = { currentDestination = it }
                    )
                }
            }
        ) {            when (currentDestination) {
                AppDestinations.HOME -> HomeScreen(
                    onNavigateToScanner = { showScanner = true }
                )
                AppDestinations.FAVORITES -> FavoritesScreen()
                AppDestinations.SAVED -> SavedScreen()
                AppDestinations.HISTORY -> HistoryScreen()
            }
        }
    }
}

enum class AppDestinations(
    val label: String,
    val icon: ImageVector,
) {
    HOME("Home", Icons.Filled.Home),
    FAVORITES("Favorites", Icons.Filled.Favorite),
    SAVED("Saved", Icons.Filled.List),
    HISTORY("History", Icons.Filled.DateRange),
}
