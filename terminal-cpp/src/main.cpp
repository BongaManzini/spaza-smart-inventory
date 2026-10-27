#include "Models.hpp"
#include "PaymentGateway.hpp"
#include "ReceiptPrinter.hpp"
#include "InventoryEngine.hpp"
#include <iostream>
#include <string>
#include <vector>
#include <iomanip>

using namespace Spaza;

void printBanner() {
    std::cout << "\n";
    std::cout << "===============================================================================\n";
    std::cout << "      🛒 SPAZA-SMART SMART TERMINAL & CARD POS ENGINE (C++17)                  \n";
    std::cout << "          South African Township Retail Decision & Settlement System           \n";
    std::cout << "               Author: Bonga Manzini (UNISA BSc Informatics)                   \n";
    std::cout << "===============================================================================\n\n";
}

void runDemo(InventoryEngine& inventory, PaymentGateway& gateway) {
    std::cout << "[DEMO] 1. Initializing Township Retail Basket...\n";
    std::vector<CartItem> cart = {
        {"MZ-125", "White Star Super Maize Meal (12.5kg)", 1, 155.0},
        {"BR-ALB", "Albany Superior Sliced White Bread (700g)", 2, 21.0},
        {"CO-02L", "Sunfoil Sunflower Cooking Oil (2L)", 1, 79.9},
        {"PL-400", "Lucky Star Pilchards in Tomato (400g)", 3, 27.5}
    };

    double total = 0.0;
    for (const auto& item : cart) {
        total += item.lineTotal();
    }
    std::cout << "   Total Basket Amount: R" << std::fixed << std::setprecision(2) << total << "\n";

    std::cout << "\n[DEMO] 2. Processing EMV Chip & PIN / NFC Contactless Card Payment...\n";
    // Simulated valid South African test card (Test Visa passing Luhn algorithm)
    CardPaymentRequest req;
    req.cardNumber = "4532015112830366"; // Valid Luhn Visa
    req.cardHolderName = "N. DLAMINI";
    req.expiryMonth = 12;
    req.expiryYear = 2028;
    req.cvv = "842";
    req.pin = "4920";
    req.amountRand = total;
    req.interfaceType = PaymentInterface::NFC_TAP_TO_PAY;
    req.merchantId = "SPAZA-SOWETO-01";

    CardPaymentResponse resp = gateway.processCardPayment(req);

    if (resp.status == PaymentStatus::APPROVED) {
        std::cout << "   ✅ Card Payment SUCCESS: " << resp.statusMessage << "\n";
        std::cout << "   Auth Code: " << resp.authorizationCode 
                  << " | RRN: " << resp.retrievalRefNumber 
                  << " | PAN: " << resp.maskedPan << "\n\n";

        // Deduct inventory
        for (const auto& item : cart) {
            inventory.recordSale(item.sku, item.quantity);
        }

        std::cout << "[DEMO] 3. Printing 80mm Thermal Till Slip (Receipt)...\n";
        std::cout << ReceiptPrinter::generateThermalTillSlip(
            "SPZ-TX-8492", "Bonga M.", "N. Dlamini", cart, resp, true
        );
    } else {
        std::cout << "   ❌ Payment FAILED: " << resp.statusMessage << "\n";
    }

    std::cout << "\n[DEMO] 4. Consolidated Weekly Wholesaler Restocking Orders...\n";
    double spend = 0.0, revenue = 0.0, profit = 0.0;
    auto orders = inventory.generateWholesaleOrders(spend, revenue, profit);
    std::cout << ReceiptPrinter::generateWeeklyOrderReport(
        orders, spend, revenue, profit, inventory.isSassaSurgeActive()
    );

    std::cout << "\n[DEMO] 5. Perishable Expiry Audit & Clearance Suggestions...\n";
    auto expiryAlerts = inventory.auditPerishables();
    std::cout << "-------------------------------------------------------------------------------\n";
    std::cout << std::left << std::setw(10) << "SKU"
              << std::setw(32) << "Item Name"
              << std::setw(12) << "Batch"
              << std::setw(12) << "Expires"
              << std::setw(10) << "Days Left"
              << std::setw(15) << "Status"
              << std::setw(10) << "Markdown" << "\n";
    std::cout << "-------------------------------------------------------------------------------\n";
    for (const auto& a : expiryAlerts) {
        std::cout << std::left << std::setw(10) << a.sku
                  << std::setw(32) << a.name.substr(0, 31)
                  << std::setw(12) << a.batchNo
                  << std::setw(12) << a.expiryDate
                  << std::right << std::setw(6) << a.daysLeft << "    "
                  << std::left << std::setw(15) << a.status
                  << std::right << std::setw(6) << std::fixed << std::setprecision(0) << a.recommendedMarkdownPct << "%\n";
    }
    std::cout << "-------------------------------------------------------------------------------\n";
}

int main(int argc, char* argv[]) {
    InventoryEngine inventory;
    PaymentGateway gateway;

    std::vector<std::string> args(argv + 1, argv + argc);

    if (args.empty() || args[0] == "--demo") {
        printBanner();
        runDemo(inventory, gateway);
        return 0;
    }

    if (args[0] == "--weekly") {
        double spend = 0.0, rev = 0.0, prof = 0.0;
        auto orders = inventory.generateWholesaleOrders(spend, rev, prof);
        std::cout << ReceiptPrinter::generateWeeklyOrderReport(orders, spend, rev, prof, inventory.isSassaSurgeActive());
        return 0;
    }

    if (args[0] == "--expiry") {
        auto alerts = inventory.auditPerishables();
        std::cout << "\n⚠️ SPAZA SMART EXPIRY AUDIT:\n";
        for (const auto& a : alerts) {
            std::cout << " • [" << a.status << "] " << a.name << " (Batch: " << a.batchNo 
                      << ") - Expiry: " << a.expiryDate << " (" << a.daysLeft << " days)";
            if (a.recommendedMarkdownPct > 0) {
                std::cout << " -> PROMPT: Apply " << a.recommendedMarkdownPct << "% clearance markdown!";
            }
            std::cout << "\n";
        }
        return 0;
    }

    if (args[0] == "--invoice") {
        std::vector<CartItem> bulkItems = {
            {"MZ-125", "White Star Super Maize Meal (12.5kg)", 5, 155.0},
            {"SG-02K", "Huletts White Sugar (2kg)", 10, 59.9},
            {"MK-06P", "First Choice Long Life Milk (6x1L)", 4, 122.0},
            {"EG-30T", "Nulaid Large Eggs Tray (30pk)", 6, 89.9}
        };
        std::cout << ReceiptPrinter::generateFormalTaxInvoice(
            "INV-2026-9901", "Orlando West Community Hall Feeding Scheme",
            "1290 Pela St, Orlando West, Soweto", "NPO-492-910", bulkItems
        );
        return 0;
    }

    if (args[0] == "--pay" && args.size() >= 2) {
        double amount = std::stod(args[1]);
        std::string cardNum = (args.size() >= 3) ? args[2] : "4532015112830366";
        std::string pin = (args.size() >= 4) ? args[3] : "1234";

        CardPaymentRequest req;
        req.cardNumber = cardNum;
        req.cardHolderName = "CUSTOMER CARDHOLDER";
        req.expiryMonth = 12;
        req.expiryYear = 2028;
        req.cvv = "123";
        req.pin = pin;
        req.amountRand = amount;
        req.interfaceType = PaymentInterface::EMV_CHIP_PIN;
        req.merchantId = "SPAZA-SOWETO-01";

        CardPaymentResponse resp = gateway.processCardPayment(req);
        if (resp.status == PaymentStatus::APPROVED) {
            std::cout << "\n✅ PAYMENT APPROVED!\n";
            std::cout << "Amount: R" << std::fixed << std::setprecision(2) << amount << "\n";
            std::cout << "Card:   " << resp.maskedPan << " (" << PaymentGateway::brandToString(resp.brand) << ")\n";
            std::cout << "Auth:   " << resp.authorizationCode << "\n";
            std::cout << "RRN:    " << resp.retrievalRefNumber << "\n";

            std::vector<CartItem> sample = {{"SALE-01", "Retail Grocery Items", 1, amount}};
            std::cout << "\n" << ReceiptPrinter::generateThermalTillSlip("TX-CLI-01", "Cashier", "Customer", sample, resp, true);
        } else {
            std::cout << "\n❌ PAYMENT DECLINED: " << resp.statusMessage << "\n";
        }
        return 0;
    }

    std::cout << "Unknown command. Use: --demo, --weekly, --expiry, --invoice, --pay <amount>\n";
    return 1;
}
