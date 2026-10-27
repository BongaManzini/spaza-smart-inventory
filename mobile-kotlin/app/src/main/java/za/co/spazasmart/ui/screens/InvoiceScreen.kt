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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import za.co.spazasmart.data.models.*
import za.co.spazasmart.data.printer.ReceiptSlipGenerator
import za.co.spazasmart.data.repository.SpazaInventoryRepository
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceScreen(
    repository: SpazaInventoryRepository,
    modifier: Modifier = Modifier
) {
    val pastSlips by repository.receiptSlips.collectAsState()
    val products by repository.products.collectAsState()

    var showCreateInvoiceDialog by remember { mutableStateOf(false) }
    var selectedInvoicePreview by remember { mutableStateOf<String?>(null) }
    var clientName by remember { mutableStateOf("Orlando West Community Creche") }
    var clientAddress by remember { mutableStateOf("1290 Pela St, Orlando West, Soweto") }
    var clientTax by remember { mutableStateOf("NPO-492-910") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Invoices & Slips Hub", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                actions = {
                    Button(
                        onClick = { showCreateInvoiceDialog = true },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("New Tax Invoice", fontSize = 11.sp)
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(12.dp)
        ) {
            Text("Recent Till Slips & Invoices", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(Modifier.height(8.dp))

            if (pastSlips.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No transactions recorded yet this session.\nUse Till POS to ring up sales.", color = Color.Gray, fontSize = 12.sp)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(pastSlips) { slip ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Slip #${slip.receiptNo} • ${slip.customerName}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("${slip.timestamp} • ${slip.paymentMode.label}", fontSize = 10.sp, color = Color.Gray)
                                    Text("Total: R${"%.2f".format(slip.totalAmount)} (${slip.items.size} items)", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                }
                                OutlinedButton(
                                    onClick = {
                                        selectedInvoicePreview = ReceiptSlipGenerator.formatThermalSlip(slip)
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Default.Receipt, contentDescription = "View", modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Reprint", fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal to create formal Tax Invoice
    if (showCreateInvoiceDialog) {
        AlertDialog(
            onDismissRequest = { showCreateInvoiceDialog = false },
            title = { Text("Generate Formal B2B Tax Invoice", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = clientName,
                        onValueChange = { clientName = it },
                        label = { Text("Client / Business Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = clientAddress,
                        onValueChange = { clientAddress = it },
                        label = { Text("Client Delivery Address") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = clientTax,
                        onValueChange = { clientTax = it },
                        label = { Text("VAT / NPO Registration Ref") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Text("Includes standard bulk community order package (Maize, Sugar, Milk, Eggs).", fontSize = 11.sp, color = Color.Gray)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val invoiceNo = "INV-${System.currentTimeMillis() % 100000}"
                        val todayStr = LocalDate.now().toString()
                        val dueStr = LocalDate.now().plusDays(7).toString()
                        val sampleItems = products.take(4).map { CartItem(it, 5) }
                        val subtotal = sampleItems.sumOf { it.lineTotal }
                        val vat = subtotal * 0.15

                        val invoice = TaxInvoice(
                            invoiceNo = invoiceNo,
                            clientName = clientName,
                            clientAddress = clientAddress,
                            clientTaxNo = clientTax,
                            items = sampleItems,
                            subtotal = subtotal,
                            vatAmount = vat,
                            totalAmount = subtotal,
                            issueDate = todayStr,
                            dueDate = dueStr
                        )

                        selectedInvoicePreview = ReceiptSlipGenerator.formatTaxInvoice(invoice)
                        showCreateInvoiceDialog = false
                    }
                ) {
                    Text("Generate & Preview")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateInvoiceDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Text / Document Preview Modal
    selectedInvoicePreview?.let { textContent ->
        AlertDialog(
            onDismissRequest = { selectedInvoicePreview = null },
            title = { Text("Document Preview", fontWeight = FontWeight.Bold) },
            text = {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().height(320.dp)
                ) {
                    LazyColumn(modifier = Modifier.padding(8.dp)) {
                        item {
                            Text(
                                textContent,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp,
                                color = Color.Black
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { selectedInvoicePreview = null }) {
                    Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Share / Print")
                }
            }
        )
    }
}
