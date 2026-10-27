#include "PaymentGateway.hpp"
#include <algorithm>
#include <cctype>
#include <random>
#include <chrono>
#include <ctime>
#include <iomanip>
#include <sstream>

namespace Spaza {

PaymentGateway::PaymentGateway() : transactionCounter(1001) {}

static std::string sanitizeCardNumber(const std::string& input) {
    std::string clean;
    for (char c : input) {
        if (std::isdigit(static_cast<unsigned char>(c))) {
            clean.push_back(c);
        }
    }
    return clean;
}

bool PaymentGateway::validateLuhn(const std::string& cardNumber) {
    std::string clean = sanitizeCardNumber(cardNumber);
    if (clean.length() < 13 || clean.length() > 19) {
        return false;
    }

    int nDigits = static_cast<int>(clean.length());
    int nSum = 0;
    bool isSecond = false;

    for (int i = nDigits - 1; i >= 0; i--) {
        int d = clean[i] - '0';
        if (isSecond) {
            d = d * 2;
            if (d > 9) {
                d -= 9;
            }
        }
        nSum += d;
        isSecond = !isSecond;
    }
    return (nSum % 10 == 0);
}

CardBrand PaymentGateway::detectCardBrand(const std::string& cardNumber) {
    std::string clean = sanitizeCardNumber(cardNumber);
    if (clean.empty()) return CardBrand::UNKNOWN;

    // SASSA Gold Cards (Postbank/SASSA Mastercards starting with 6005, 6011, 5050)
    if (clean.rfind("6005", 0) == 0 || clean.rfind("5050", 0) == 0) {
        return CardBrand::SASSA_GOLD_DEBIT;
    }

    // Visa starts with 4
    if (clean[0] == '4') {
        return CardBrand::VISA;
    }

    // Mastercard starts with 51-55 or 2221-2720
    if (clean.length() >= 2) {
        int prefix2 = std::stoi(clean.substr(0, 2));
        if (prefix2 >= 51 && prefix2 <= 55) {
            return CardBrand::MASTERCARD;
        }
    }
    if (clean.length() >= 4) {
        int prefix4 = std::stoi(clean.substr(0, 4));
        if (prefix4 >= 2221 && prefix4 <= 2720) {
            return CardBrand::MASTERCARD;
        }
    }

    // Amex starts with 34 or 37
    if (clean.length() >= 2) {
        int prefix2 = std::stoi(clean.substr(0, 2));
        if (prefix2 == 34 || prefix2 == 37) {
            return CardBrand::AMEX;
        }
    }

    return CardBrand::UNKNOWN;
}

std::string PaymentGateway::brandToString(CardBrand brand) {
    switch (brand) {
        case CardBrand::VISA: return "VISA DEBIT/CREDIT";
        case CardBrand::MASTERCARD: return "MASTERCARD";
        case CardBrand::SASSA_GOLD_DEBIT: return "SASSA POSTBANK GOLD CARD";
        case CardBrand::AMEX: return "AMERICAN EXPRESS";
        default: return "STANDARD EMV DEBIT";
    }
}

std::string PaymentGateway::maskPan(const std::string& cardNumber) {
    std::string clean = sanitizeCardNumber(cardNumber);
    if (clean.length() < 10) return "****";
    std::string last4 = clean.substr(clean.length() - 4);
    std::string first4 = clean.substr(0, 4);
    return first4 + " **** **** " + last4;
}

std::string PaymentGateway::generateAuthCode() {
    static const char digits[] = "0123456789";
    std::string auth = "AUTH";
    for (int i = 0; i < 6; i++) {
        auth += digits[rand() % 10];
    }
    return auth;
}

std::string PaymentGateway::generateRRN() {
    // Retrieval Reference Number (12 digits)
    std::string rrn;
    for (int i = 0; i < 12; i++) {
        rrn += std::to_string(rand() % 10);
    }
    return rrn;
}

std::string PaymentGateway::generateSTAN() {
    transactionCounter++;
    std::ostringstream ss;
    ss << std::setw(6) << std::setfill('0') << (transactionCounter % 1000000);
    return ss.str();
}

std::string PaymentGateway::getCurrentIsoTimestamp() {
    auto now = std::chrono::system_clock::now();
    std::time_t tt = std::chrono::system_clock::to_time_t(now);
    std::tm tm{};
    localtime_r(&tt, &tm);

    std::ostringstream ss;
    ss << std::put_time(&tm, "%Y-%m-%d %H:%M:%S");
    return ss.str();
}

CardPaymentResponse PaymentGateway::processCardPayment(const CardPaymentRequest& request) {
    CardPaymentResponse resp;
    resp.amountPaid = request.amountRand;
    resp.timestamp = getCurrentIsoTimestamp();
    resp.brand = detectCardBrand(request.cardNumber);
    resp.maskedPan = maskPan(request.cardNumber);

    std::string clean = sanitizeCardNumber(request.cardNumber);

    // 1. Validate Luhn checksum
    if (!validateLuhn(clean)) {
        resp.status = PaymentStatus::DECLINED_INVALID_CARD_NUMBER;
        resp.statusMessage = "DECLINED: Invalid Card Checksum (Luhn Failure)";
        return resp;
    }

    // 2. Validate Card Expiration
    auto now = std::chrono::system_clock::now();
    std::time_t tt = std::chrono::system_clock::to_time_t(now);
    std::tm tm{};
    localtime_r(&tt, &tm);
    int currentYear = 1900 + tm.tm_year;
    int currentMonth = 1 + tm.tm_mon;

    if (request.expiryYear < currentYear || 
       (request.expiryYear == currentYear && request.expiryMonth < currentMonth)) {
        resp.status = PaymentStatus::DECLINED_EXPIRED_CARD;
        resp.statusMessage = "DECLINED: Card Has Expired";
        return resp;
    }

    // 3. Validate PIN for Chip/PIN or SASSA card
    if (request.interfaceType == PaymentInterface::EMV_CHIP_PIN || resp.brand == CardBrand::SASSA_GOLD_DEBIT) {
        if (request.pin.length() != 4 && request.pin.length() != 5) {
            resp.status = PaymentStatus::DECLINED_INVALID_PIN;
            resp.statusMessage = "DECLINED: Invalid PIN Entry";
            return resp;
        }
    }

    // 4. Check negative limits / insufficient funds simulation (amount > R50,000)
    if (request.amountRand > 50000.0) {
        resp.status = PaymentStatus::DECLINED_INSUFFICIENT_FUNDS;
        resp.statusMessage = "DECLINED: Exceeds Township Card Limit / Insufficient Funds";
        return resp;
    }

    // 5. Approved via BankservAfrica / PASA gateway
    resp.status = PaymentStatus::APPROVED;
    resp.statusMessage = "TRANSACTION APPROVED - ISSUER OK";
    resp.authorizationCode = generateAuthCode();
    resp.retrievalRefNumber = generateRRN();
    resp.systemTraceAudit = generateSTAN();

    return resp;
}

} // namespace Spaza
