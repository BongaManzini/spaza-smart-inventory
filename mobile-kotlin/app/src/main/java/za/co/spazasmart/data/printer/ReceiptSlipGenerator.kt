package za.co.spazasmart.data.printer

import za.co.spazasmart.data.models.*

object ReceiptSlipGenerator {

    /**
     * Generates a thermal receipt slip formatted for 58mm/80mm mobile ESC/POS printers.
     */
    fun formatThermalSlip(slip: TillSlip): String {
        val sb = StringBuilder()
        sb.appendLine("==========================================")
        sb.appendLine("     🏪 SOWETO SMART SPAZA & WHOLESALE    ")
        sb.appendLine("       Vilakazi Street, Orlando West      ")
        sb.appendLine("            Soweto, Gauteng 1804          ")
        sb.appendLine("           VAT REG: 4920192841            ")
        sb.appendLine("            Tel: (011) 935-1829           ")
        sb.appendLine("==========================================")
        sb.appendLine("Date:    ${slip.timestamp}")
        sb.appendLine("Slip No: #${slip.receiptNo} | Cashier: ${slip.cashierName}")
        sb.appendLine("Cust:    ${slip.customerName.ifEmpty { "Walk-in Township Customer" }}")
        sb.appendLine("------------------------------------------")
        sb.appendLine(String.format("%-22s %3s %7s %7s", "Item", "Qty", "Price", "Total"))
        sb.appendLine("------------------------------------------")

        for (item in slip.items) {
            val shortName = if (item.product.name.length > 21) item.product.name.take(21) else item.product.name
            sb.appendLine(
                String.format(
                    "%-22s %3d %7.2f %7.2f",
                    shortName,
                    item.quantity,
                    item.product.sellingPriceRand,
                    item.lineTotal
                )
            )
        }

        sb.appendLine("------------------------------------------")
        sb.appendLine(String.format("%-25s R%14.2f", "SUBTOTAL (EXCL VAT):", slip.subtotal - slip.vatAmount))
        sb.appendLine(String.format("%-25s R%14.2f", "VAT (15% INCL):", slip.vatAmount))
        sb.appendLine(String.format("%-25s R%14.2f", "TOTAL PAYABLE:", slip.totalAmount))
        sb.appendLine("==========================================")

        when (slip.paymentMode) {
            PaymentMode.CARD_CONTACTLESS, PaymentMode.CARD_CHIP_PIN, PaymentMode.SASSA_CARD -> {
                sb.appendLine("PAYMENT METHOD: CARD TRANSACTION")
                slip.cardPaymentResult?.let { card ->
                    sb.appendLine("CARD SCHEME:    ${card.brand.displayName}")
                    sb.appendLine("CARD PAN:       ${card.maskedPan}")
                    sb.appendLine("AUTH CODE:      ${card.authCode} (APPROVED)")
                    sb.appendLine("RRN:            ${card.rrn}")
                    sb.appendLine("STAN:           ${card.stan}")
                    sb.appendLine("TERMINAL:       SPAZA-ANDROID-POS-01 (PASA)")
                }
            }
            PaymentMode.CASH -> {
                sb.appendLine("PAYMENT METHOD: CASH")
                sb.appendLine(String.format("%-25s R%14.2f", "CASH TENDERED:", slip.cashTendered))
                sb.appendLine(String.format("%-25s R%14.2f", "CHANGE DUE:", slip.changeDue))
            }
            PaymentMode.CAPITEC_PAY -> {
                sb.appendLine("PAYMENT METHOD: CAPITEC PAY / INSTANT EFT")
                sb.appendLine("SETTLEMENT:     DIRECT MERCHANT INSTANT CLEAR")
            }
        }

        sb.appendLine("------------------------------------------")
        sb.appendLine("       SIYABONGA! ENKOSI! DANKIE!         ")
        sb.appendLine("    Thank you for supporting Local Spaza  ")
        sb.appendLine("      Keep slip for returns in 7 days     ")
        sb.appendLine("            [ |||||||||||||||||| ]        ")
        sb.appendLine("==========================================")

        return sb.toString()
    }

    /**
     * Formats a formal A4 Tax Invoice for corporate, NGO, or creche catering clients.
     */
    fun formatTaxInvoice(invoice: TaxInvoice): String {
        val sb = StringBuilder()
        sb.appendLine("===============================================================================")
        sb.appendLine("                                 TAX INVOICE                                   ")
        sb.appendLine("===============================================================================")
        sb.appendLine("ISSUED BY:")
        sb.appendLine("  SPAZA-SMART INFORMAL RETAIL & TRADING (PTY) LTD")
        sb.appendLine("  Registration No: 2024/918274/07 | VAT No: 4920192841")
        sb.appendLine("  1824 Vilakazi Street, Soweto, Johannesburg, 1804")
        sb.appendLine("  Email: orders@spazasmart.co.za | Tel: +27 (011) 935-1829")
        sb.appendLine("-------------------------------------------------------------------------------")
        sb.appendLine("INVOICE NUMBER: ${invoice.invoiceNo.padEnd(26)} DATE: ${invoice.issueDate}")
        sb.appendLine("PAYMENT TERMS:  ${invoice.paymentTerms.padEnd(26)} DUE DATE: ${invoice.dueDate}")
        sb.appendLine("-------------------------------------------------------------------------------")
        sb.appendLine("BILLED TO:")
        sb.appendLine("  Client Name:    ${invoice.clientName}")
        sb.appendLine("  Address:        ${invoice.clientAddress}")
        sb.appendLine("  Tax / Ref:      ${invoice.clientTaxNo}")
        sb.appendLine("-------------------------------------------------------------------------------")
        sb.appendLine(String.format("%-10s %-38s %5s %12s %14s", "SKU", "Item Description", "Qty", "Unit (ZAR)", "Total (ZAR)"))
        sb.appendLine("-------------------------------------------------------------------------------")

        for (item in invoice.items) {
            val desc = if (item.product.name.length > 37) item.product.name.take(37) else item.product.name
            sb.appendLine(
                String.format(
                    "%-10s %-38s %5d %12.2f %14.2f",
                    item.product.sku,
                    desc,
                    item.quantity,
                    item.product.sellingPriceRand,
                    item.lineTotal
                )
            )
        }

        sb.appendLine("-------------------------------------------------------------------------------")
        sb.appendLine(String.format("%-63s R%14.2f", "Subtotal (Excl. VAT):", invoice.subtotal - invoice.vatAmount))
        sb.appendLine(String.format("%-63s R%14.2f", "Value Added Tax (15%):", invoice.vatAmount))
        sb.appendLine(String.format("%-63s R%14.2f", "TOTAL INVOICE AMOUNT DUE:", invoice.totalAmount))
        sb.appendLine("===============================================================================")
        sb.appendLine("BANKING DETAILS FOR INSTANT EFT SETTLEMENT:")
        sb.appendLine("  Bank:           Capitec Business / First National Bank (FNB)")
        sb.appendLine("  Account Name:   Spaza-Smart Retail Enterprises")
        sb.appendLine("  Account Number: 1058291048")
        sb.appendLine("  Branch Code:    250655 (FNB) / 470010 (Capitec)")
        sb.appendLine("  Payment Ref:    ${invoice.invoiceNo}")
        sb.appendLine("===============================================================================")
        return sb.toString()
    }
}
