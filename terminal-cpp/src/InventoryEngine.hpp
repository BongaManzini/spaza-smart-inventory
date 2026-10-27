#pragma once

#include "Models.hpp"
#include <vector>
#include <string>
#include <map>

namespace Spaza {

class InventoryEngine {
public:
    InventoryEngine();

    // Returns all products in the staple basket
    const std::vector<SpazaProduct>& getProducts() const { return products; }

    // Check if currently in SASSA grant disbursement window (days 1-6)
    bool isSassaSurgeActive() const;
    void setSassaSurgeManual(bool active);

    // Decrements stock when an item is purchased
    bool recordSale(const std::string& sku, int quantity);

    // Calculates runout and reorder thresholds
    struct ForecastItem {
        SpazaProduct product;
        double effectiveDailySales;
        double daysRemaining;
        bool needsReorder;
        int recommendedWeeklyOrder;
        double weeklyOrderCost;
    };
    std::vector<ForecastItem> generateForecast() const;

    // Generates consolidated orders for wholesale suppliers
    std::vector<WholesalerOrder> generateWholesaleOrders(double& outTotalSpend, double& outProjectedRevenue, double& outProjectedProfit) const;

    // Checks batch expiry statuses
    struct ExpiryAlert {
        std::string sku;
        std::string name;
        std::string batchNo;
        std::string expiryDate;
        int daysLeft;
        std::string status;          // "EXPIRED", "CRITICAL (<3d)", "WARNING (<7d)", "SAFE"
        double recommendedMarkdownPct; // Clearance discount %
    };
    std::vector<ExpiryAlert> auditPerishables() const;

private:
    std::vector<SpazaProduct> products;
    bool forceSassaSurge;
    bool hasManualSurgeOverride;
};

} // namespace Spaza
