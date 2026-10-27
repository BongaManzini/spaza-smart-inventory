#pragma once

#include "Models.hpp"
#include <string>

namespace Spaza {

class PaymentGateway {
public:
    PaymentGateway();

    // Validates credit/debit card numbers using the standard Luhn algorithm (mod 10)
    static bool validateLuhn(const std::string& cardNumber);

    // Determines card scheme (Visa, Mastercard, SASSA Gold Card, etc.)
    static CardBrand detectCardBrand(const std::string& cardNumber);
    static std::string brandToString(CardBrand brand);

    // Masks 16-digit PAN (e.g., **** **** **** 4812)
    static std::string maskPan(const std::string& cardNumber);

    // Authorizes transaction via simulated South African banking switch (BankservAfrica / PASA)
    CardPaymentResponse processCardPayment(const CardPaymentRequest& request);

private:
    std::string generateAuthCode();
    std::string generateRRN();
    std::string generateSTAN();
    std::string getCurrentIsoTimestamp();
    int transactionCounter;
};

} // namespace Spaza
