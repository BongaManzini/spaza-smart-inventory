package za.co.spazasmart.ui.screens

import androidx.compose.foundation.background
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
import za.co.spazasmart.data.payment.CardPaymentProcessor
import za.co.spazasmart.data.printer.ReceiptSlipGenerator
import za.co.spazasmart.data.repository.SpazaInventoryRepository
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosScreen(
    repository: SpazaInventoryRepository,
    modifier: Modifier = Modifier
) {
    val products by repository.products.collectAsState()
    val isSassaSurge by repository.isSassaSurgeActive.collectAsState()

    val cart = remember { mutableStateListOf<CartItem>() }
    var showPaymentDialog by remember { mutableStateOf(false) }
    var activePaymentMode by remember { mutableStateOf(PaymentMode.CARD_CONTACTLESS) }
    var showReceiptSlipDialog by remember { mutableStateOf<TillSlip?>(null) }

    // Card input states
    var cardNumber by remember { mutableStateOf("4532015112830366") }
    var cardHolder by remember { mutableStateOf("N. DLAMINI") }
    var expiryMonth by remember { mutableStateOf("12") }
    var expiryYear by remember { mutableStateOf("2028") }
    var cvv by remember { mutableStateOf("842") }
    var pin by remember { mutableStateOf("4920") }
    var paymentErrorMessage by remember { mutableStateOf<String?>(null) }
    var cashPaid by remember { mutableStateOf("200.00") }

    val totalAmount = cart.sumOf { it.lineTotal }
    val vatAmount = totalAmount * 0.15
    val subtotalExcl = totalAmount - vatAmount

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Spaza-Smart Mobile Till POS", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            if (isSassaSurge) "🔥 SASSA Surge Active (Days 1–6)" else "🟢 Mid-Month Normal Turnover",
                            fontSize = 11.sp,
                            color = if (isSassaSurge) Color(0xFFD97706) else Color(0xFF059669)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { repository.toggleSassaSurge(!isSassaSurge) }) {
                        Icon(Icons.Default.Bolt, contentDescription = "Toggle Surge", tint = if (isSassaSurge) Color(0xFFD97706) else Color.Gray)
                    }
                }
            )
        }
    ) { paddingValues ->
        Row(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Left Column: Catalog Staples
            Column(modifier = Modifier.weight(1.1f)) {
                Text("Staple Grocery Basket", fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(bottom = 6.dp))
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(products) { product ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(product.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1)
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text("R${"%.2f".format(product.sellingPriceRand)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                                        Text("Stock: ${product.currentStock} ${product.unit}", fontSize = 11.sp, color = if (product.currentStock <= 5) Color.Red else Color.Gray)
                                    }
                                }
                                Button(
                                    onClick = {
                                        val existing = cart.indexOfFirst { it.product.sku == product.sku }
                                        if (existing != -1) {
                                            cart[existing] = cart[existing].copy(quantity = cart[existing].quantity + 1)
                                        } else {
                                            cart.add(CartItem(product, 1))
                                        }
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.AddShoppingCart, contentDescription = "Add", modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Right Column: Cart & Payment Checkout
            Card(
                modifier = Modifier
                    .weight(0.9f)
                    .fillMaxHeight(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Current Cart", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            if (cart.isNotEmpty()) {
                                TextButton(onClick = { cart.clear() }) {
                                    Text("Clear", color = Color.Red, fontSize = 12.sp)
                                }
                            }
                        }

                        Divider(modifier = Modifier.padding(vertical = 4.dp))

                        if (cart.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Cart is empty\nTap + to add groceries", color = Color.Gray, fontSize = 12.sp)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(cart) { item ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(item.product.name, fontSize = 11.sp, maxLines = 1)
                                            Text("${item.quantity} x R${"%.2f".format(item.product.sellingPriceRand)} = R${"%.2f".format(item.lineTotal)}", fontSize = 10.sp, color = Color.Gray)
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(
                                                onClick = {
                                                    val idx = cart.indexOf(item)
                                                    if (item.quantity > 1) {
                                                        cart[idx] = item.copy(quantity = item.quantity - 1)
                                                    } else {
                                                        cart.remove(item)
                                                    }
                                                },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(Icons.Default.Remove, contentDescription = "Decrease", modifier = Modifier.size(14.dp))
                                            }
                                            Text("${item.quantity}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            IconButton(
                                                onClick = {
                                                    val idx = cart.indexOf(item)
                                                    cart[idx] = item.copy(quantity = item.quantity + 1)
                                                },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(Icons.Default.Add, contentDescription = "Increase", modifier = Modifier.size(14.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Totals & Checkout Button
                    Column {
                        Divider()
                        Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Subtotal:", fontSize = 12.sp, color = Color.Gray)
                            Text("R${"%.2f".format(subtotalExcl)}", fontSize = 12.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("VAT (15%):", fontSize = 12.sp, color = Color.Gray)
                            Text("R${"%.2f".format(vatAmount)}", fontSize = 12.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total Due:", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("R${"%.2f".format(totalAmount)}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                        }

                        Button(
                            onClick = { showPaymentDialog = true },
                            enabled = cart.isNotEmpty(),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.CreditCard, contentDescription = "Pay", modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Tender Payment (R${"%.2f".format(totalAmount)})")
                        }
                    }
                }
            }
        }
    }

    // Payment Processing Modal
    if (showPaymentDialog) {
        AlertDialog(
            onDismissRequest = { showPaymentDialog = false },
            title = { Text("Tender Customer Payment", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Payable Amount: R${"%.2f".format(totalAmount)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                    // Payment Method Tabs
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        FilterChip(
                            selected = activePaymentMode == PaymentMode.CARD_CONTACTLESS,
                            onClick = { activePaymentMode = PaymentMode.CARD_CONTACTLESS },
                            label = { Text("Card (Tap)", fontSize = 10.sp) }
                        )
                        FilterChip(
                            selected = activePaymentMode == PaymentMode.SASSA_CARD,
                            onClick = {
                                activePaymentMode = PaymentMode.SASSA_CARD
                                cardNumber = "6005829104829104" // Standard SASSA Gold Card BIN
                            },
                            label = { Text("SASSA Gold", fontSize = 10.sp) }
                        )
                        FilterChip(
                            selected = activePaymentMode == PaymentMode.CASH,
                            onClick = { activePaymentMode = PaymentMode.CASH },
                            label = { Text("Cash", fontSize = 10.sp) }
                        )
                    }

                    if (activePaymentMode == PaymentMode.CARD_CONTACTLESS || activePaymentMode == PaymentMode.SASSA_CARD) {
                        val brand = CardPaymentProcessor.detectCardBrand(cardNumber)
                        Text("Card Scheme: ${brand.displayName}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.secondary)

                        OutlinedTextField(
                            value = cardNumber,
                            onValueChange = { cardNumber = it },
                            label = { Text("16-Digit Card PAN") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedTextField(
                                value = expiryMonth,
                                onValueChange = { expiryMonth = it },
                                label = { Text("MM") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = expiryYear,
                                onValueChange = { expiryYear = it },
                                label = { Text("YYYY") },
                                modifier = Modifier.weight(1.5f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = cvv,
                                onValueChange = { cvv = it },
                                label = { Text("CVV") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }

                        OutlinedTextField(
                            value = pin,
                            onValueChange = { pin = it },
                            label = { Text("Customer PIN (4-digit)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    } else {
                        OutlinedTextField(
                            value = cashPaid,
                            onValueChange = { cashPaid = it },
                            label = { Text("Cash Received (ZAR)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        val tendered = cashPaid.toDoubleOrNull() ?: 0.0
                        val change = (tendered - totalAmount).coerceAtLeast(0.0)
                        Text("Change to Return: R${"%.2f".format(change)}", fontWeight = FontWeight.Bold, color = Color(0xFF059669))
                    }

                    paymentErrorMessage?.let { err ->
                        Text(err, color = Color.Red, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val nowTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                        val slipNo = "SPZ-${System.currentTimeMillis() % 100000}"

                        if (activePaymentMode == PaymentMode.CASH) {
                            val tendered = cashPaid.toDoubleOrNull() ?: totalAmount
                            val slip = TillSlip(
                                receiptNo = slipNo,
                                cashierName = "Bonga M.",
                                customerName = "Township Customer",
                                items = cart.toList(),
                                subtotal = subtotalExcl,
                                vatAmount = vatAmount,
                                totalAmount = totalAmount,
                                paymentMode = PaymentMode.CASH,
                                cashTendered = tendered,
                                changeDue = (tendered - totalAmount).coerceAtLeast(0.0),
                                timestamp = nowTime
                            )
                            repository.recordSale(cart.toList(), slip)
                            cart.clear()
                            showPaymentDialog = false
                            showReceiptSlipDialog = slip
                        } else {
                            val card = CardDetails(
                                cardNumber = cardNumber,
                                cardHolder = cardHolder,
                                expiryMonth = expiryMonth.toIntOrNull() ?: 12,
                                expiryYear = expiryYear.toIntOrNull() ?: 2028,
                                cvv = cvv,
                                pin = pin,
                                isContactless = (activePaymentMode == PaymentMode.CARD_CONTACTLESS)
                            )
                            val res = CardPaymentProcessor.processPayment(card, totalAmount)
                            if (res.status == PaymentStatus::APPROVED.get()) {
                                val slip = TillSlip(
                                    receiptNo = slipNo,
                                    cashierName = "Bonga M.",
                                    customerName = cardHolder,
                                    items = cart.toList(),
                                    subtotal = subtotalExcl,
                                    vatAmount = vatAmount,
                                    totalAmount = totalAmount,
                                    paymentMode = activePaymentMode,
                                    cardPaymentResult = res,
                                    timestamp = nowTime
                                )
                                repository.recordSale(cart.toList(), slip)
                                cart.clear()
                                showPaymentDialog = false
                                paymentErrorMessage = null
                                showReceiptSlipDialog = slip
                            } else {
                                paymentErrorMessage = res.statusMessage
                            }
                        }
                    }
                ) {
                    Text("Authorize & Print Slip")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPaymentDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Receipt Thermal Slip Print Modal
    showReceiptSlipDialog?.let { slip ->
        val slipText = ReceiptSlipGenerator.formatThermalSlip(slip)
        AlertDialog(
            onDismissRequest = { showReceiptSlipDialog = null },
            title = { Text("Receipt Slip (80mm Thermal)", fontWeight = FontWeight.Bold) },
            text = {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().height(320.dp)
                ) {
                    LazyColumn(modifier = Modifier.padding(8.dp)) {
                        item {
                            Text(
                                slipText,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = Color.Black
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showReceiptSlipDialog = null }) {
                    Icon(Icons.Default.Print, contentDescription = "Print", modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Done / Bluetooth Print")
                }
            }
        )
    }
}
