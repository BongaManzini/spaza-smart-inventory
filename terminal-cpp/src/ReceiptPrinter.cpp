#include "ReceiptPrinter.hpp"
#include "PaymentGateway.hpp"
#include <iomanip>
#include <sstream>
#include <chrono>
#include <ctime>

namespace Spaza {

static std::string formatCurrency(double amount) {
    std::ostringstream ss;
    ss << "R" << std::fixed << std::setprecision(2) << amount;
    return ss.str();
}

std::string ReceiptPrinter::generateThermalTillSlip(
    const std::string& receiptNo,
    const std::string& cashierName,
    const std::string& customerName,
    const std::vector<CartItem>& items,
    const CardPaymentResponse& paymentResponse,
    bool isCardPayment,
    double cashTendered
) {
    std::ostringstream ss;
    double subtotal = 0.0;
    for (const auto& item : items) {
        subtotal += item.lineTotal();
    }
    double vat = subtotal * 0.15;
    double total = subtotal;

    ss << "==========================================\n";
    ss << "     🏪 SOWETO SMART SPAZA & WHOLESALE     \n";
    ss << "       Vilakazi Street, Orlando West       \n";
    ss << "            Soweto, Gauteng 1804           \n";
    ss << "           VAT REG: 4920192841             \n";
    ss << "            Tel: (011) 935-1829            \n";
    ss << "==========================================\n";
    ss << "Date:    " << paymentResponse.timestamp << "\n";
    ss << "Slip No: #" << receiptNo << " | Cashier: " << cashierName << "\n";
    ss << "Cust:    " << (customerName.empty() ? "Walk-in Township Customer" : customerName) << "\n";
    ss << "------------------------------------------\n";
    ss << std::left << std::setw(22) << "Item"
       << std::right << std::setw(3) << "Qty"
       << std::setw(8) << "Price"
       << std::setw(9) << "Total" << "\n";
    ss << "------------------------------------------\n";

    for (const auto& item : items) {
        std::string shortName = item.name.substr(0, 21);
        ss << std::left << std::setw(22) << shortName
           << std::right << std::setw(3) << item.quantity
           << std::setw(8) << std::fixed << std::setprecision(2) << item.unitPrice
           << std::setw(9) << std::fixed << std::setprecision(2) << item.lineTotal() << "\n";
    }

    ss << "------------------------------------------\n";
    ss << std::left << std::setw(25) << "SUBTOTAL (EXCL VAT):" 
       << std::right << std::setw(17) << formatCurrency(subtotal - vat) << "\n";
    ss << std::left << std::setw(25) << "VAT (15% INCL):" 
       << std::right << std::setw(17) << formatCurrency(vat) << "\n";
    ss << std::left << std::setw(25) << "TOTAL PAYABLE:" 
       << std::right << std::setw(17) << formatCurrency(total) << "\n";
    ss << "==========================================\n";

    if (isCardPayment) {
        ss << "PAYMENT METHOD: CARD TRANSACTION\n";
        ss << "CARD SCHEME:    " << PaymentGateway::brandToString(paymentResponse.brand) << "\n";
        ss << "CARD PAN:       " << paymentResponse.maskedPan << "\n";
        ss << "AUTH CODE:      " << paymentResponse.authorizationCode << " (APPROVED)\n";
        ss << "RRN:            " << paymentResponse.retrievalRefNumber << "\n";
        ss << "STAN / TRACE:   " << paymentResponse.systemTraceAudit << "\n";
        ss << "TERMINAL:       SPAZA-EMV-POS-01 (CAPITEC/PASA)\n";
    } else {
        ss << "PAYMENT METHOD: CASH TENDERED\n";
        ss << "CASH PAID:      " << formatCurrency(cashTendered) << "\n";
        ss << "CHANGE DUE:     " << formatCurrency(cashTendered - total) << "\n";
    }

    ss << "------------------------------------------\n";
    ss << "       SIYABONGA! ENKOSI! DANKIE!         \n";
    ss << "    Thank you for supporting Local Spaza  \n";
    ss << "      Keep slip for returns in 7 days     \n";
    ss << "            [ |||||||||||||||||| ]        \n";
    ss << "==========================================\n";

    return ss.str();
}

std::string ReceiptPrinter::generateFormalTaxInvoice(
    const std::string& invoiceNo,
    const std::string& clientName,
    const std::string& clientAddress,
    const std::string& clientTaxNo,
    const std::vector<CartItem>& items,
    const std::string& paymentTerms
) {
    std::ostringstream ss;
    double subtotal = 0.0;
    for (const auto& item : items) {
        subtotal += item.lineTotal();
    }
    double vat = subtotal * 0.15;
    double total = subtotal;

    auto now = std::chrono::system_clock::now();
    std::time_t tt = std::chrono::system_clock::to_time_t(now);
    std::tm tm{};
    localtime_r(&tt, &tm);

    char dateBuf[64];
    std::strftime(dateBuf, sizeof(dateBuf), "%d %B %Y", &tm);

    ss << "===============================================================================\n";
    ss << "                                 TAX INVOICE                                   \n";
    ss << "===============================================================================\n";
    ss << "ISSUED BY:\n";
    ss << "  SPAZA-SMART INFORMAL RETAIL & TRADING (PTY) LTD\n";
    ss << "  Registration No: 2024/918274/07 | VAT No: 4920192841\n";
    ss << "  1824 Vilakazi Street, Soweto, Johannesburg, 1804\n";
    ss << "  Email: orders@spazasmart.co.za | Tel: +27 (011) 935-1829\n";
    ss << "-------------------------------------------------------------------------------\n";
    ss << "INVOICE NUMBER: " << std::left << std::setw(26) << invoiceNo 
       << "DATE: " << dateBuf << "\n";
    ss << "PAYMENT TERMS:  " << std::left << std::setw(26) << paymentTerms
       << "STATUS: ISSUED & PENDING SETTLEMENT\n";
    ss << "-------------------------------------------------------------------------------\n";
    ss << "BILLED TO:\n";
    ss << "  Client Name:    " << clientName << "\n";
    ss << "  Delivery Addr:  " << clientAddress << "\n";
    ss << "  VAT / NPO Ref:  " << clientTaxNo << "\n";
    ss << "-------------------------------------------------------------------------------\n";
    ss << std::left << std::setw(10) << "SKU"
       << std::setw(38) << "Item Description"
       << std::right << std::setw(5) << "Qty"
       << std::setw(12) << "Unit (ZAR)"
       << std::setw(14) << "Total (ZAR)" << "\n";
    ss << "-------------------------------------------------------------------------------\n";

    for (const auto& item : items) {
        std::string shortDesc = item.name.substr(0, 37);
        ss << std::left << std::setw(10) << item.sku
           << std::setw(38) << shortDesc
           << std::right << std::setw(5) << item.quantity
           << std::setw(12) << std::fixed << std::setprecision(2) << item.unitPrice
           << std::setw(14) << std::fixed << std::setprecision(2) << item.lineTotal() << "\n";
    }

    ss << "-------------------------------------------------------------------------------\n";
    ss << std::left << std::setw(63) << "Subtotal (Excl. VAT):"
       << std::right << std::setw(16) << formatCurrency(subtotal - vat) << "\n";
    ss << std::left << std::setw(63) << "Value Added Tax (15%):"
       << std::right << std::setw(16) << formatCurrency(vat) << "\n";
    ss << std::left << std::setw(63) << "TOTAL INVOICE AMOUNT DUE:"
       << std::right << std::setw(16) << formatCurrency(total) << "\n";
    ss << "===============================================================================\n";
    ss << "BANKING DETAILS FOR INSTANT EFT SETTLEMENT:\n";
    ss << "  Bank:           Capitec Business / First National Bank (FNB)\n";
    ss << "  Account Name:   Spaza-Smart Retail Enterprises\n";
    ss << "  Account Number: 1058291048\n";
    ss << "  Branch Code:    250655 (FNB) / 470010 (Capitec)\n";
    ss << "  Payment Ref:    " << invoiceNo << "\n";
    ss << "===============================================================================\n";
    ss << "Authorized Signature: _______________________      Date: " << dateBuf << "\n";

    return ss.str();
}

std::string ReceiptPrinter::generateWeeklyOrderReport(
    const std::vector<WholesalerOrder>& orders,
    double totalSpend,
    double projectedRevenue,
    double projectedProfit,
    bool isSassaWeek
) {
    std::ostringstream ss;
    ss << "===============================================================================\n";
    ss << "            🛒 SPAZA WEEKLY WHOLESALER RESTOCK & REQUISITION SHEET             \n";
    ss << "===============================================================================\n";
    ss << "SASSA Grant Cycle Status: " 
       << (isSassaWeek ? "🔥 ACTIVE HIGH SURGE (Grant Disbursement Days 1-6)" : "🟢 MID-MONTH NORMAL TURNOVER") << "\n";
    ss << "Total Wholesaler Spend:   " << formatCurrency(totalSpend) << "\n";
    ss << "Projected Retail Revenue: " << formatCurrency(projectedRevenue) << "\n";
    ss << "Estimated Gross Profit:   " << formatCurrency(projectedProfit) 
       << " (" << std::fixed << std::setprecision(1) << (totalSpend > 0 ? (projectedProfit / projectedRevenue * 100.0) : 0.0) << "% Margin)\n";
    ss << "-------------------------------------------------------------------------------\n";

    for (const auto& ord : orders) {
        if (ord.totalUnits == 0) continue;
        ss << "\n🏢 WHOLESALER: " << ord.wholesalerName << "\n";
        ss << "   Consolidated Requisition Value: " << formatCurrency(ord.totalCost) 
           << " | Total Units: " << ord.totalUnits << "\n";
        ss << "   " << std::left << std::setw(10) << "SKU"
           << std::setw(35) << "Item Name"
           << std::right << std::setw(6) << "Units"
           << std::setw(12) << "Wholesale"
           << std::setw(14) << "Subtotal" << "\n";
        ss << "   --------------------------------------------------------------------\n";
        for (const auto& itm : ord.items) {
            std::string nameCut = itm.name.substr(0, 34);
            ss << "   " << std::left << std::setw(10) << itm.sku
               << std::setw(35) << nameCut
               << std::right << std::setw(6) << itm.quantity
               << std::setw(12) << std::fixed << std::setprecision(2) << itm.unitPrice
               << std::setw(14) << std::fixed << std::setprecision(2) << itm.lineTotal() << "\n";
        }
    }
    ss << "===============================================================================\n";
    return ss.str();
}

} // namespace Spaza
