package za.co.spazasmart.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import za.co.spazasmart.data.repository.SpazaInventoryRepository
import za.co.spazasmart.ui.screens.*

enum class AppDestination(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    POS("Till POS", Icons.Default.PointOfSale),
    INVENTORY("Inventory", Icons.Default.Inventory2),
    INVOICES("Invoices & Slips", Icons.Default.ReceiptLong),
    WEEKLY("Weekly Orders", Icons.Default.Analytics)
}

@Composable
fun SpazaSmartApp(
    repository: SpazaInventoryRepository,
    modifier: Modifier = Modifier
) {
    var currentDestination by remember { mutableStateOf(AppDestination.POS) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                AppDestination.values().forEach { destination ->
                    NavigationBarItem(
                        selected = currentDestination == destination,
                        onClick = { currentDestination = destination },
                        icon = { Icon(destination.icon, contentDescription = destination.label) },
                        label = { Text(destination.label) }
                    )
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        when (currentDestination) {
            AppDestination.POS -> PosScreen(repository = repository, modifier = Modifier.padding(innerPadding))
            AppDestination.INVENTORY -> InventoryExpiryScreen(repository = repository, modifier = Modifier.padding(innerPadding))
            AppDestination.INVOICES -> InvoiceScreen(repository = repository, modifier = Modifier.padding(innerPadding))
            AppDestination.WEEKLY -> WeeklyReportScreen(repository = repository, modifier = Modifier.padding(innerPadding))
        }
    }
}
