package za.co.spazasmart.data.models

import java.time.LocalDate

enum class CardBrand(val displayName: String) {
    VISA("Visa Debit/Credit"),
    MASTERCARD("Mastercard"),
    SASSA_GOLD_DEBIT("SASSA Postbank Gold Card"),
    AMEX("American Express"),
    UNKNOWN("Standard Debit")
}

enum class PaymentStatus {
    APPROVED,
    DECLINED_INSUFFICIENT_FUNDS,
    DECLINED_INVALID_PIN,
    DECLINED_INVALID_CARD,
    DECLINED_EXPIRED,
    ERROR
}

enum class PaymentMode(val label: String) {
    CARD_CONTACTLESS("Card (Tap / NFC)"),
    CARD_CHIP_PIN("Card (Chip & PIN)"),
    SASSA_CARD("SASSA Gold Card"),
    CASH("Cash Tender"),
    CAPITEC_PAY("Capitec Pay / QR")
}

data class SpazaProduct(
    val sku: String,
    val name: String,
    val category: String,
    val unit: String,
    val currentStock: Int,
    val dailyBaseSales: Double,
    val sassaMultiplier: Double,
    val unitCostRand: Double,
    val sellingPriceRand: Double,
    val supplierLeadDays: Int,
    val supplier: String,
    val batchNo: String,
    val expiryDate: LocalDate
) {
    fun daysUntilExpiry(today: LocalDate = LocalDate.now()): Long {
        return java.time.temporal.ChronoUnit.DAYS.between(today, expiryDate)
    }

    fun isUrgentExpiry(today: LocalDate = LocalDate.now()): Boolean {
        return daysUntilExpiry(today) <= 3
    }

    fun recommendedMarkdownPct(today: LocalDate = LocalDate.now()): Int {
        val days = daysUntilExpiry(today)
        return when {
            days < 0 -> 100 // Expired
            days <= 3 -> 25 // Clearance
            days <= 7 -> 10
            else -> 0
        }
    }
}

data class CartItem(
    val product: SpazaProduct,
    val quantity: Int
) {
    val lineTotal: Double get() = quantity * product.sellingPriceRand
}

data class CardDetails(
    val cardNumber: String,
    val cardHolder: String,
    val expiryMonth: Int,
    val expiryYear: Int,
    val cvv: String,
    val pin: String = "",
    val isContactless: Boolean = true
)

data class CardPaymentResult(
    val status: PaymentStatus,
    val statusMessage: String,
    val authCode: String,
    val rrn: String,
    val stan: String,
    val maskedPan: String,
    val brand: CardBrand,
    val amountPaid: Double,
    val timestamp: String
)

data class TillSlip(
    val receiptNo: String,
    val cashierName: String,
    val customerName: String,
    val items: List<CartItem>,
    val subtotal: Double,
    val vatAmount: Double,
    val totalAmount: Double,
    val paymentMode: PaymentMode,
    val cardPaymentResult: CardPaymentResult? = null,
    val cashTendered: Double = 0.0,
    val changeDue: Double = 0.0,
    val timestamp: String
)

data class TaxInvoice(
    val invoiceNo: String,
    val clientName: String,
    val clientAddress: String,
    val clientTaxNo: String,
    val items: List<CartItem>,
    val subtotal: Double,
    val vatAmount: Double,
    val totalAmount: Double,
    val issueDate: String,
    val dueDate: String,
    val paymentTerms: String = "7 Days / Instant EFT"
)

data class WholesalerOrder(
    val wholesalerName: String,
    val items: List<CartItem>,
    val totalUnits: Int,
    val totalCost: Double
)

data class WeeklyReport(
    val date: String,
    val isSassaSurgeActive: Boolean,
    val totalUnitsToOrder: Int,
    val totalWholesaleCost: Double,
    val totalProjectedRevenue: Double,
    val projectedGrossProfit: Double,
    val ordersByWholesaler: List<WholesalerOrder>
)
