package za.co.spazasmart.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import za.co.spazasmart.data.repository.SpazaInventoryRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryExpiryScreen(
    repository: SpazaInventoryRepository,
    modifier: Modifier = Modifier
) {
    val products by repository.products.collectAsState()
    val isSassaSurge by repository.isSassaSurgeActive.collectAsState()

    var selectedTab by remember { mutableStateOf(0) } // 0 = Stockout Predictor, 1 = Expiry Tracker

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Inventory & Expiry Audit", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                actions = {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 8.dp)) {
                        Text(if (isSassaSurge) "SASSA Week" else "Normal", fontSize = 11.sp)
                        Switch(
                            checked = isSassaSurge,
                            onCheckedChange = { repository.toggleSassaSurge(it) },
                            modifier = Modifier.padding(start = 6.dp)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 12.dp)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Stockout Forecast", fontSize = 12.sp) },
                    icon = { Icon(Icons.Default.Inventory2, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Expiry & Perishables", fontSize = 12.sp) },
                    icon = { Icon(Icons.Default.HourglassEmpty, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
            }

            Spacer(Modifier.height(8.dp))

            if (selectedTab == 0) {
                // Stockout Forecast Tab
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(products) { prod ->
                        val velocity = prod.dailyBaseSales * (if (isSassaSurge) prod.sassaMultiplier else 1.0)
                        val daysRemaining = if (velocity > 0) prod.currentStock / velocity else 999.0
                        val isUrgent = daysRemaining < 2.0
                        val reorderUnits = ((velocity * 7) - prod.currentStock).coerceAtLeast(0.0).toInt()

                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(prod.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text("SKU: ${prod.sku} • Supplier: ${prod.supplier}", fontSize = 10.sp, color = Color.Gray)
                                    }
                                    if (isUrgent) {
                                        AssistChip(
                                            onClick = {},
                                            label = { Text("🚨 REORDER", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Red) },
                                            colors = AssistChipDefaults.assistChipColors(containerColor = Color(0xFFFEE2E2))
                                        )
                                    } else {
                                        AssistChip(
                                            onClick = {},
                                            label = { Text("✅ OK", fontSize = 9.sp, color = Color(0xFF059669)) },
                                            colors = AssistChipDefaults.assistChipColors(containerColor = Color(0xFFD1FAE5))
                                        )
                                    }
                                }

                                Spacer(Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Stock: ${prod.currentStock} ${prod.unit}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    Text("Burn: ${"%.1f".format(velocity)} ${prod.unit}/day", fontSize = 12.sp, color = if (isSassaSurge) Color(0xFFD97706) else Color.Gray)
                                    Text("Runout: ${"%.1f".format(daysRemaining)} days", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isUrgent) Color.Red else Color(0xFF059669))
                                }

                                if (reorderUnits > 0) {
                                    Divider(modifier = Modifier.padding(vertical = 6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Reorder for 7 Days: $reorderUnits ${prod.unit} (~R${"%.2f".format(reorderUnits * prod.unitCostRand)})", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                        OutlinedButton(
                                            onClick = { repository.restockProduct(prod.sku, reorderUnits) },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text("Restock", fontSize = 10.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Expiry & Perishables Tab
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(products) { prod ->
                        val daysLeft = prod.daysUntilExpiry()
                        val isCritical = daysLeft <= 3
                        val isWarning = daysLeft in 4..7
                        val markdownPct = prod.recommendedMarkdownPct()

                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = when {
                                    daysLeft < 0 -> Color(0xFFFEE2E2)
                                    isCritical -> Color(0xFFFEF3C7)
                                    else -> MaterialTheme.colorScheme.surface
                                }
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(prod.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("Batch: ${prod.batchNo} • Expiry: ${prod.expiryDate}", fontSize = 10.sp, color = Color.Gray)
                                    }
                                    Text(
                                        when {
                                            daysLeft < 0 -> "EXPIRED"
                                            daysLeft <= 3 -> "Expires in $daysLeft days!"
                                            else -> "$daysLeft days left"
                                        },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = if (isCritical) Color(0xFFDC2626) else if (isWarning) Color(0xFFD97706) else Color(0xFF059669)
                                    )
                                }

                                if (markdownPct > 0) {
                                    Spacer(Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Clearance Markdown: Apply $markdownPct% Discount", fontSize = 11.sp, color = Color(0xFFB45309), fontWeight = FontWeight.SemiBold)
                                        Button(
                                            onClick = { repository.applyClearanceMarkdown(prod.sku, markdownPct) },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706))
                                        ) {
                                            Text("Apply Markdown", fontSize = 10.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
