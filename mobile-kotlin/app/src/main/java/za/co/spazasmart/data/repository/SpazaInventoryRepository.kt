package za.co.spazasmart.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import za.co.spazasmart.data.models.*
import java.time.LocalDate

class SpazaInventoryRepository {

    private val _products = MutableStateFlow<List<SpazaProduct>>(initialBasket())
    val products: StateFlow<List<SpazaProduct>> = _products.asStateFlow()

    private val _isSassaSurgeActive = MutableStateFlow(isSassaDisbursementWeek())
    val isSassaSurgeActive: StateFlow<Boolean> = _isSassaSurgeActive.asStateFlow()

    private val _receiptSlips = MutableStateFlow<List<TillSlip>>(emptyList())
    val receiptSlips: StateFlow<List<TillSlip>> = _receiptSlips.asStateFlow()

    fun toggleSassaSurge(active: Boolean) {
        _isSassaSurgeActive.value = active
    }

    fun recordSale(items: List<CartItem>, slip: TillSlip) {
        val currentList = _products.value.toMutableList()
        for (cartItem in items) {
            val idx = currentList.indexOfFirst { it.sku == cartItem.product.sku }
            if (idx != -1) {
                val p = currentList[idx]
                val newStock = (p.currentStock - cartItem.quantity).coerceAtLeast(0)
                currentList[idx] = p.copy(currentStock = newStock)
            }
        }
        _products.value = currentList

        // Store slip in history
        _receiptSlips.value = listOf(slip) + _receiptSlips.value
    }

    fun restockProduct(sku: String, units: Int) {
        val currentList = _products.value.toMutableList()
        val idx = currentList.indexOfFirst { it.sku == sku }
        if (idx != -1) {
            val p = currentList[idx]
            currentList[idx] = p.copy(currentStock = p.currentStock + units)
            _products.value = currentList
        }
    }

    fun applyClearanceMarkdown(sku: String, discountPct: Int) {
        val currentList = _products.value.toMutableList()
        val idx = currentList.indexOfFirst { it.sku == sku }
        if (idx != -1) {
            val p = currentList[idx]
            val discounted = p.sellingPriceRand * (1.0 - (discountPct / 100.0))
            currentList[idx] = p.copy(sellingPriceRand = discounted)
            _products.value = currentList
        }
    }

    fun generateWeeklyReport(): WeeklyReport {
        val prods = _products.value
        val isSurge = _isSassaSurgeActive.value
        val ordersMap = mutableMapOf<String, MutableList<CartItem>>()

        var totalSpend = 0.0
        var totalRev = 0.0
        var totalUnits = 0

        for (p in prods) {
            val velocity = p.dailyBaseSales * (if (isSurge) p.sassaMultiplier else 1.0)
            val weeklyDemand = velocity * 7
            val reorderUnits = (weeklyDemand - p.currentStock).coerceAtLeast(0.0).toInt()

            if (reorderUnits > 0) {
                val list = ordersMap.getOrPut(p.supplier) { mutableListOf() }
                list.add(CartItem(p, reorderUnits))
                totalUnits += reorderUnits
                totalSpend += reorderUnits * p.unitCostRand
                totalRev += reorderUnits * p.sellingPriceRand
            }
        }

        val wholesalerOrders = ordersMap.map { (supplier, items) ->
            val cost = items.sumOf { it.quantity * it.product.unitCostRand }
            val units = items.sumOf { it.quantity }
            WholesalerOrder(supplier, items, units, cost)
        }

        return WeeklyReport(
            date = LocalDate.now().toString(),
            isSassaSurgeActive = isSurge,
            totalUnitsToOrder = totalUnits,
            totalWholesaleCost = totalSpend,
            totalProjectedRevenue = totalRev,
            projectedGrossProfit = totalRev - totalSpend,
            ordersByWholesaler = wholesalerOrders
        )
    }

    companion object {
        fun isSassaDisbursementWeek(today: LocalDate = LocalDate.now()): Boolean {
            return today.dayOfMonth in 1..6
        }

        fun initialBasket(): List<SpazaProduct> {
            val today = LocalDate.now()
            return listOf(
                SpazaProduct("MZ-125", "White Star Super Maize Meal (12.5kg)", "Staple Grain", "Bags", 8, 4.2, 4.5, 125.0, 155.0, 2, "Devland Cash & Carry", "LOT-MZ881", today.plusDays(180)),
                SpazaProduct("BR-ALB", "Albany Superior Sliced White Bread (700g)", "Bakery Daily", "Loaves", 15, 35.0, 1.8, 17.5, 21.0, 1, "Boxer Superstores", "LOT-BR042", today.plusDays(2)),
                SpazaProduct("CO-02L", "Sunfoil Sunflower Cooking Oil (2L)", "Cooking Essentials", "Bottles", 6, 3.0, 3.8, 64.0, 79.9, 2, "Makro Crown Mines", "LOT-CO912", today.plusDays(240)),
                SpazaProduct("SG-02K", "Huletts White Sugar (2kg)", "Dry Pantry", "Packs", 10, 5.0, 3.0, 48.0, 59.9, 2, "Devland Cash & Carry", "LOT-SG401", today.plusDays(365)),
                SpazaProduct("RC-10K", "Tastic Parboiled Rice (10kg)", "Staple Grain", "Bags", 5, 2.8, 3.6, 175.0, 215.0, 3, "Makro Crown Mines", "LOT-RC773", today.plusDays(300)),
                SpazaProduct("MK-06P", "First Choice Long Life Milk (6x1L)", "Dairy", "Boxes", 4, 3.5, 2.5, 98.0, 122.0, 2, "Boxer Superstores", "LOT-MK303", today.plusDays(45)),
                SpazaProduct("PL-400", "Lucky Star Pilchards in Tomato (400g)", "Canned Protein", "Cans", 24, 12.0, 2.8, 22.5, 27.5, 1, "Devland Cash & Carry", "LOT-PL109", today.plusDays(700)),
                SpazaProduct("EG-30T", "Nulaid Large Eggs Tray (30pk)", "Poultry & Dairy", "Trays", 7, 6.0, 2.2, 72.0, 89.9, 1, "Boxer Superstores", "LOT-EG550", today.plusDays(5))
            )
        }
    }
}
