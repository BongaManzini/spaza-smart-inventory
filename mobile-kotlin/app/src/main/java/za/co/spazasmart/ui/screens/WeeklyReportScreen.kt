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
fun WeeklyReportScreen(
    repository: SpazaInventoryRepository,
    modifier: Modifier = Modifier
) {
    val isSassaSurge by repository.isSassaSurgeActive.collectAsState()
    val weeklyReport = remember(isSassaSurge) { repository.generateWeeklyReport() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Weekly Shop Restock & Health", fontWeight = FontWeight.Bold, fontSize = 18.sp) }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Summary KPI Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Weekly Purchasing Forecast", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            AssistChip(
                                onClick = {},
                                label = {
                                    Text(
                                        if (weeklyReport.isSassaSurgeActive) "🔥 SASSA SURGE WEEK" else "🟢 NORMAL TURNOVER",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp
                                    )
                                }
                            )
                        }

                        Spacer(Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Wholesale Spend", fontSize = 11.sp, color = Color.Gray)
                                Text("R${"%.2f".format(weeklyReport.totalWholesaleCost)}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                            Column {
                                Text("Projected Sales", fontSize = 11.sp, color = Color.Gray)
                                Text("R${"%.2f".format(weeklyReport.totalProjectedRevenue)}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                            Column {
                                Text("Est. Gross Profit", fontSize = 11.sp, color = Color.Gray)
                                Text("R${"%.2f".format(weeklyReport.projectedGrossProfit)}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF059669))
                            }
                        }

                        Spacer(Modifier.height(4.dp))
                        val margin = if (weeklyReport.totalProjectedRevenue > 0) (weeklyReport.projectedGrossProfit / weeklyReport.totalProjectedRevenue) * 100 else 0.0
                        Text("Projected Profit Margin: ${"%.1f".format(margin)}% • Total Items Ordered: ${weeklyReport.totalUnitsToOrder} units", fontSize = 11.sp)
                    }
                }
            }

            // Wholesaler Section Header
            item {
                Text("Requisitions Grouped By Wholesaler", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }

            // Orders list
            items(weeklyReport.ordersByWholesaler) { order ->
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
                            Column {
                                Text(order.wholesalerName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("${order.totalUnits} units • Total: R${"%.2f".format(order.totalCost)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                            }
                            Button(
                                onClick = { /* Bluetooth print or share purchase order */ },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Order Sheet", fontSize = 10.sp)
                            }
                        }

                        Divider(modifier = Modifier.padding(vertical = 6.dp))

                        order.items.forEach { item ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("${item.quantity}x ${item.product.name}", fontSize = 11.sp, modifier = Modifier.weight(1f))
                                Text("R${"%.2f".format(item.lineTotal)}", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        }
    }
}
