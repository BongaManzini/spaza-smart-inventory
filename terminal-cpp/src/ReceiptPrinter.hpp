#pragma once

#include "Models.hpp"
#include <string>
#include <vector>

namespace Spaza {

class ReceiptPrinter {
public:
    static std::string generateThermalTillSlip(
        const std::string& receiptNo,
        const std::string& cashierName,
        const std::string& customerName,
        const std::vector<CartItem>& items,
        const CardPaymentResponse& paymentResponse,
        bool isCardPayment = true,
        double cashTendered = 0.0
    );

    static std::string generateFormalTaxInvoice(
        const std::string& invoiceNo,
        const std::string& clientName,
        const std::string& clientAddress,
        const std::string& clientTaxNo,
        const std::vector<CartItem>& items,
        const std::string& paymentTerms = "7 Days / Instant EFT"
    );

    static std::string generateWeeklyOrderReport(
        const std::vector<WholesalerOrder>& orders,
        double totalSpend,
        double projectedRevenue,
        double projectedProfit,
        bool isSassaWeek
    );
};

} // namespace Spaza
