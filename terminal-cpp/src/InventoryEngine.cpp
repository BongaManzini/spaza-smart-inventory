#include "InventoryEngine.hpp"
#include <cmath>
#include <chrono>
#include <ctime>
#include <algorithm>

namespace Spaza {

InventoryEngine::InventoryEngine() : forceSassaSurge(false), hasManualSurgeOverride(false) {
    products = {
        {"MZ-125", "White Star Maize Meal (12.5kg)", "Staple Grain", "Bags", 8, 4.2, 4.5, 125.0, 155.0, 2, "Devland Cash & Carry", "LOT-MZ881", "2027-04-25", 180},
        {"BR-ALB", "Albany Sliced White Bread (700g)", "Bakery Daily", "Loaves", 15, 35.0, 1.8, 17.5, 21.0, 1, "Boxer Superstores", "LOT-BR042", "2026-10-29", 2},
        {"CO-02L", "Sunfoil Sunflower Oil (2L)", "Cooking Essentials", "Bottles", 6, 3.0, 3.8, 64.0, 79.9, 2, "Makro Crown Mines", "LOT-CO912", "2027-06-24", 240},
        {"SG-02K", "Huletts White Sugar (2kg)", "Dry Pantry", "Packs", 10, 5.0, 3.0, 48.0, 59.9, 2, "Devland Cash & Carry", "LOT-SG401", "2027-10-27", 365},
        {"RC-10K", "Tastic Parboiled Rice (10kg)", "Staple Grain", "Bags", 5, 2.8, 3.6, 175.0, 215.0, 3, "Makro Crown Mines", "LOT-RC773", "2027-08-23", 300},
        {"MK-06P", "First Choice Long Life Milk (6x1L)", "Dairy", "Boxes", 4, 3.5, 2.5, 98.0, 122.0, 2, "Boxer Superstores", "LOT-MK303", "2026-12-11", 45},
        {"PL-400", "Lucky Star Pilchards (400g)", "Canned Protein", "Cans", 24, 12.0, 2.8, 22.5, 27.5, 1, "Devland Cash & Carry", "LOT-PL109", "2028-09-26", 700},
        {"EG-30T", "Nulaid Large Eggs Tray (30pk)", "Poultry & Dairy", "Trays", 7, 6.0, 2.2, 72.0, 89.9, 1, "Boxer Superstores", "LOT-EG550", "2026-11-01", 5}
    };
}

bool InventoryEngine::isSassaSurgeActive() const {
    if (hasManualSurgeOverride) {
        return forceSassaSurge;
    }
    auto now = std::chrono::system_clock::now();
    std::time_t tt = std::chrono::system_clock::to_time_t(now);
    std::tm tm{};
    localtime_r(&tt, &tm);
    return (tm.tm_mday >= 1 && tm.tm_mday <= 6);
}

void InventoryEngine::setSassaSurgeManual(bool active) {
    forceSassaSurge = active;
    hasManualSurgeOverride = true;
}

bool InventoryEngine::recordSale(const std::string& sku, int quantity) {
    for (auto& p : products) {
        if (p.sku == sku) {
            if (p.currentStock >= quantity) {
                p.currentStock -= quantity;
                return true;
            } else {
                p.currentStock = 0;
                return true;
            }
        }
    }
    return false;
}

std::vector<InventoryEngine::ForecastItem> InventoryEngine::generateForecast() const {
    std::vector<ForecastItem> results;
    bool surge = isSassaSurgeActive();

    for (const auto& prod : products) {
        double velocity = prod.dailyBaseSales * (surge ? prod.sassaMultiplier : 1.0);
        double daysLeft = (velocity > 0.0) ? (static_cast<double>(prod.currentStock) / velocity) : 999.0;
        double reorderThreshold = velocity * (prod.supplierLeadDays + 1);
        bool needsReorder = (prod.currentStock <= reorderThreshold);

        double weeklyDemand = velocity * 7.0;
        int orderUnits = std::max(0, static_cast<int>(std::round(weeklyDemand - prod.currentStock)));

        results.push_back({
            prod,
            velocity,
            daysLeft,
            needsReorder,
            orderUnits,
            orderUnits * prod.unitCostRand
        });
    }

    return results;
}

std::vector<WholesalerOrder> InventoryEngine::generateWholesaleOrders(
    double& outTotalSpend, 
    double& outProjectedRevenue, 
    double& outProjectedProfit
) const {
    auto forecasts = generateForecast();
    std::map<std::string, WholesalerOrder> orderMap;

    outTotalSpend = 0.0;
    outProjectedRevenue = 0.0;

    for (const auto& f : forecasts) {
        if (orderMap.find(f.product.supplier) == orderMap.end()) {
            orderMap[f.product.supplier] = {f.product.supplier, {}, 0.0, 0};
        }

        if (f.recommendedWeeklyOrder > 0) {
            CartItem itm;
            itm.sku = f.product.sku;
            itm.name = f.product.name;
            itm.quantity = f.recommendedWeeklyOrder;
            itm.unitPrice = f.product.unitCostRand;

            orderMap[f.product.supplier].items.push_back(itm);
            orderMap[f.product.supplier].totalCost += itm.lineTotal();
            orderMap[f.product.supplier].totalUnits += itm.quantity;

            outTotalSpend += itm.lineTotal();
            outProjectedRevenue += (itm.quantity * f.product.sellingPriceRand);
        }
    }

    outProjectedProfit = outProjectedRevenue - outTotalSpend;

    std::vector<WholesalerOrder> resultList;
    for (const auto& kv : orderMap) {
        resultList.push_back(kv.second);
    }
    return resultList;
}

std::vector<InventoryEngine::ExpiryAlert> InventoryEngine::auditPerishables() const {
    std::vector<ExpiryAlert> alerts;

    for (const auto& p : products) {
        std::string status;
        double markdown = 0.0;

        if (p.daysUntilExpiry < 0) {
            status = "EXPIRED";
            markdown = 100.0;
        } else if (p.daysUntilExpiry <= 3) {
            status = "CRITICAL (<3d)";
            markdown = 25.0; // Suggest 25% discount to clear stock today
        } else if (p.daysUntilExpiry <= 7) {
            status = "WARNING (<7d)";
            markdown = 10.0;
        } else {
            status = "SAFE";
            markdown = 0.0;
        }

        alerts.push_back({
            p.sku,
            p.name,
            p.batchNo,
            p.expiryDate,
            p.daysUntilExpiry,
            status,
            markdown
        });
    }

    return alerts;
}

} // namespace Spaza
