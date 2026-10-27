#pragma once

#include <string>
#include <vector>
#include <chrono>
#include <iomanip>
#include <sstream>

namespace Spaza {

enum class CardBrand {
    VISA,
    MASTERCARD,
    SASSA_GOLD_DEBIT,
    AMEX,
    UNKNOWN
};

enum class PaymentStatus {
    APPROVED,
    DECLINED_INSUFFICIENT_FUNDS,
    DECLINED_INVALID_PIN,
    DECLINED_INVALID_CARD_NUMBER,
    DECLINED_EXPIRED_CARD,
    NETWORK_ERROR
};

enum class PaymentInterface {
    NFC_TAP_TO_PAY,
    EMV_CHIP_PIN,
    MAGSTRIPE_SWIPE
};

struct CardPaymentRequest {
    std::string cardNumber;
    std::string cardHolderName;
    int expiryMonth;
    int expiryYear;
    std::string cvv;
    std::string pin;
    double amountRand;
    PaymentInterface interfaceType;
    std::string merchantId;
};

struct CardPaymentResponse {
    PaymentStatus status;
    std::string statusMessage;
    std::string authorizationCode;
    std::string retrievalRefNumber; // RRN
    std::string systemTraceAudit;   // STAN
    std::string maskedPan;
    CardBrand brand;
    double amountPaid;
    std::string timestamp;
};

struct SpazaProduct {
    std::string sku;
    std::string name;
    std::string category;
    std::string unit;
    int currentStock;
    double dailyBaseSales;
    double sassaMultiplier;
    double unitCostRand;
    double sellingPriceRand;
    int supplierLeadDays;
    std::string supplier;
    std::string batchNo;
    std::string expiryDate;
    int daysUntilExpiry;
};

struct CartItem {
    std::string sku;
    std::string name;
    int quantity;
    double unitPrice;
    double lineTotal() const { return quantity * unitPrice; }
};

struct WholesalerOrder {
    std::string wholesalerName;
    std::vector<CartItem> items;
    double totalCost;
    int totalUnits;
};

} // namespace Spaza
