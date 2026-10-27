package za.co.spazasmart.data.payment

import za.co.spazasmart.data.models.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.random.Random

object CardPaymentProcessor {

    private var transactionCounter = 2001

    /**
     * Standard Luhn checksum algorithm (Mod 10) for Visa, Mastercard & SASSA cards.
     */
    fun validateLuhn(cardNumber: String): Boolean {
        val clean = cardNumber.filter { it.isDigit() }
        if (clean.length !in 13..19) return false

        var sum = 0
        var isSecond = false
        for (i in clean.length - 1 downTo 0) {
            var d = clean[i] - '0'
            if (isSecond) {
                d *= 2
                if (d > 9) d -= 9
            }
            sum += d
            isSecond = !isSecond
        }
        return (sum % 10 == 0)
    }

    /**
     * Identifies card scheme, specifically recognizing South African SASSA Postbank Gold cards.
     */
    fun detectCardBrand(cardNumber: String): CardBrand {
        val clean = cardNumber.filter { it.isDigit() }
        if (clean.isEmpty()) return CardBrand.UNKNOWN

        // Postbank SASSA Gold Debit Card BINs
        if (clean.startsWith("6005") || clean.startsWith("5050") || clean.startsWith("6011")) {
            return CardBrand.SASSA_GOLD_DEBIT
        }
        // Visa
        if (clean.startsWith("4")) {
            return CardBrand.VISA
        }
        // Mastercard
        val firstTwo = clean.take(2).toIntOrNull() ?: 0
        if (firstTwo in 51..55) return CardBrand.MASTERCARD
        val firstFour = clean.take(4).toIntOrNull() ?: 0
        if (firstFour in 2221..2720) return CardBrand.MASTERCARD

        // Amex
        if (firstTwo == 34 || firstTwo == 37) return CardBrand.AMEX

        return CardBrand.UNKNOWN
    }

    /**
     * Masks PAN to protect customer card data (PCI-DSS compliance standard).
     */
    fun maskPan(cardNumber: String): String {
        val clean = cardNumber.filter { it.isDigit() }
        if (clean.length < 10) return "**** **** **** ****"
        val first4 = clean.take(4)
        val last4 = clean.takeLast(4)
        return "$first4 **** **** $last4"
    }

    /**
     * Processes card payment with simulated EMV & South African Bank switch (BankservAfrica/PASA).
     */
    fun processPayment(card: CardDetails, amountRand: Double): CardPaymentResult {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        val brand = detectCardBrand(card.cardNumber)
        val masked = maskPan(card.cardNumber)

        // 1. Luhn Validation
        if (!validateLuhn(card.cardNumber)) {
            return CardPaymentResult(
                status = PaymentStatus.DECLINED_INVALID_CARD,
                statusMessage = "DECLINED: Invalid card number (Luhn Checksum Failure)",
                authCode = "",
                rrn = "",
                stan = "",
                maskedPan = masked,
                brand = brand,
                amountPaid = amountRand,
                timestamp = timestamp
            )
        }

        // 2. Expiry Validation
        val now = LocalDateTime.now()
        val currentYear = now.year
        val currentMonth = now.monthValue
        if (card.expiryYear < currentYear || (card.expiryYear == currentYear && card.expiryMonth < currentMonth)) {
            return CardPaymentResult(
                status = PaymentStatus.DECLINED_EXPIRED,
                statusMessage = "DECLINED: Card has expired",
                authCode = "",
                rrn = "",
                stan = "",
                maskedPan = masked,
                brand = brand,
                amountPaid = amountRand,
                timestamp = timestamp
            )
        }

        // 3. PIN Check (Required for Chip & PIN or SASSA cards)
        if (!card.isContactless || brand == CardBrand.SASSA_GOLD_DEBIT) {
            if (card.pin.length !in 4..5) {
                return CardPaymentResult(
                    status = PaymentStatus.DECLINED_INVALID_PIN,
                    statusMessage = "DECLINED: Invalid PIN entry",
                    authCode = "",
                    rrn = "",
                    stan = "",
                    maskedPan = masked,
                    brand = brand,
                    amountPaid = amountRand,
                    timestamp = timestamp
                )
            }
        }

        // 4. Spending limit simulation
        if (amountRand > 50_000.0) {
            return CardPaymentResult(
                status = PaymentStatus.DECLINED_INSUFFICIENT_FUNDS,
                statusMessage = "DECLINED: Amount exceeds township retail limit / Insufficient Funds",
                authCode = "",
                rrn = "",
                stan = "",
                maskedPan = masked,
                brand = brand,
                amountPaid = amountRand,
                timestamp = timestamp
            )
        }

        // 5. Approved via BankservAfrica / PASA gateway
        transactionCounter++
        val authCode = "AUTH" + (100000 + Random.nextInt(900000))
        val rrn = (100000000000L + Random.nextLong(900000000000L)).toString()
        val stan = String.format("%06d", transactionCounter % 1_000_000)

        return CardPaymentResult(
            status = PaymentStatus.APPROVED,
            statusMessage = "TRANSACTION APPROVED - ISSUER OK",
            authCode = authCode,
            rrn = rrn,
            stan = stan,
            maskedPan = masked,
            brand = brand,
            amountPaid = amountRand,
            timestamp = timestamp
        )
    }
}
