# 🛒 Spaza-Smart Retail Hub & Inventory Predictor

> **Live Web Application**: [https://bongamanzini.github.io/spaza-smart-inventory/](https://bongamanzini.github.io/spaza-smart-inventory/)

An enterprise-ready, township-tailored retail decision-support system, Point-of-Sale (POS) terminal, and inventory forecasting platform engineered for South African Spaza shops, tuckshops, and micro-retailers.

Spaza-Smart connects staple inventory stockout prediction to the monthly **South African Social Security Agency (SASSA) grant payout cycle**, equipped with **Card and Contactless Payments**, **Thermal Till Slip Printing (80mm / 58mm)**, **Official A4 Tax Invoices**, **Perishable Expiry Date Monitoring**, and native **Android (Kotlin)** & **Embedded Terminal (C++17)** applications.

---

## 🎯 The Informatics & Informal Economy Angle

- **The Problem**: Informal township Spaza shop owners lose up to **18% of their gross monthly earnings** because they run out of basic staple goods (maize meal, cooking oil, bread, sugar) during days 1 to 6 of the month when millions of SASSA grant beneficiaries receive and spend their funds. Furthermore, up to **7% of inventory is lost to unnoticed perishable expiry** (bread, dairy, eggs), and manual paper till slips lead to bookkeeping reconciliation errors.
- **The Informatics Solution**:
  - **Dynamic SASSA Demand Multipliers**: Models daily SKU consumption velocity across payout surges (300% to 450% velocity increase on staple food items).
  - **Wholesaler Order Consolidation**: Aggregates 7-day purchasing needs mapped to regional Cash & Carry suppliers (**Devland Cash & Carry**, **Boxer Superstores**, **Makro Crown Mines**).
  - **Perishable Expiry & Markdown Engine**: Tracks batch lot numbers and alerts when items expire in $<3$ days, proposing automated clearance discounts (e.g., 25% markdown) to prevent write-offs.
  - **Slip & Invoicing Suite**: Issues instant 80mm thermal receipt slips and formal A4 Tax Invoices for bulk orders (creches, township feeding schemes, catering).
  - **Multi-Platform Deployment**: Unified across Web POS, Python CLI, Android Kotlin Mobile POS, and Embedded C++ POS Terminals.

---

## 🏗️ Multi-Platform Architecture

```
Spaza-Smart-Inventory/
├── index.html                  # Responsive Web POS & Decision Support App (Live on GitHub Pages)
├── spaza_inventory_predictor.py # Enhanced Python CLI Engine & Report Formatter
├── requirements.txt            # Python dependencies (pandas, numpy)
│
├── terminal-cpp/               # ⚡ High-Performance Embedded C++17 POS Terminal Engine
│   ├── CMakeLists.txt          # Modern CMake build configuration
│   ├── Makefile                # Fast 'make' build script
│   └── src/
│       ├── main.cpp            # Interactive terminal POS & CLI flags (--demo, --pay, --invoice)
│       ├── Models.hpp          # Product, Cart, Payment & Invoice data structures
│       ├── PaymentGateway.hpp  # Luhn algorithm mod-10 & BankservAfrica / PASA gateway
│       ├── PaymentGateway.cpp  # Card scheme detection (Visa, Mastercard, SASSA Gold Card)
│       ├── ReceiptPrinter.hpp  # ESC/POS 80mm thermal slip & A4 Tax Invoice formatter
│       ├── ReceiptPrinter.cpp  # Receipt formatting logic
│       ├── InventoryEngine.hpp # Stock velocity & perishable expiry auditor
│       └── InventoryEngine.cpp # Inventory math & markdown logic
│
└── mobile-kotlin/              # 📱 Native Android Kotlin Mobile App (Jetpack Compose)
    ├── build.gradle.kts        # Root Gradle build script
    ├── settings.gradle.kts     # Project settings
    ├── README.md               # Mobile app guide & hardware POS setup
    └── app/
        ├── build.gradle.kts    # Material 3, Compose BOM, StateFlow, Coroutines
        └── src/main/
            ├── AndroidManifest.xml # Permissions (NFC, Bluetooth, Internet)
            └── java/za/co/spazasmart/
                ├── MainActivity.kt # Android ComponentActivity entry point
                ├── data/
                │   ├── models/SpazaModels.kt            # Clean architecture data models
                │   ├── payment/CardPaymentProcessor.kt  # Luhn algorithm, EMV Chip & NFC Tap
                │   ├── printer/ReceiptSlipGenerator.kt  # Thermal ESC/POS receipt slips
                │   └── repository/SpazaInventoryRepository.kt # Reactive StateFlow inventory
                └── ui/
                    ├── SpazaSmartApp.kt                 # Material 3 Bottom Navigation Scaffold
                    ├── screens/PosScreen.kt             # Till POS, Card & Cash payments, Slips
                    ├── screens/InventoryExpiryScreen.kt # SASSA stockout forecast & expiry audit
                    ├── screens/InvoiceScreen.kt         # B2B Tax Invoice generator & archive
                    ├── screens/WeeklyReportScreen.kt    # Wholesaler orders & margin analytics
                    └── theme/Theme.kt                   # South African retail color palette
```

---

## 🛠️ Feature Breakdown

### 1. Point of Sale (POS) & Card Payments
- **Card Scheme Support**: Visa, Mastercard, and **SASSA Postbank Gold Debit Cards** (BIN `6005`, `5050`, `6011`).
- **Luhn Algorithm Checksum**: Real-time validation of 16-digit Primary Account Numbers (PAN).
- **Payment Modes**:
  - **NFC Contactless Tap-to-Phone**: Instant card tap processing.
  - **EMV Chip & PIN**: PIN verification with simulated bank authorization switch.
  - **Cash Tender**: Quick denomination buttons (R50, R100, R200, R500) and change calculation.
- **Card Protection**: Compliant masking (`4532 **** **** 0366`) on printed slips and transaction records.

### 2. Thermal Till Slips & Formal A4 Tax Invoices
- **80mm / 58mm Thermal Receipt Slips**:
  - Store details: *Soweto Smart Spaza & Wholesale, Vilakazi Street, Soweto*.
  - Itemized table with quantities, prices, 15% VAT breakdown, and payment approval codes.
  - Local township multilingual signoff: *"Siyabonga! Enkosi! Dankie!"* with verification barcode.
- **Formal A4 Tax Invoices**:
  - Official invoice header with Company Registration and VAT numbers.
  - Billed to client details (e.g. *Orlando West Community Creche & Feeding Scheme*).
  - Instant EFT settlement instructions with South African banking details (Capitec Business / FNB).
  - Clean browser printing with specialized `@media print` rules.

### 3. Perishable Expiry Tracker & Wastage Mitigation
- Batch monitoring (`LOT-MZ881`, `LOT-BR042`, etc.) with countdown of days until expiry.
- Color-coded alert badges:
  - 🔴 **Critical ($\le 3$ days)**: Immediate clearance recommendation.
  - 🟡 **Warning ($4-7$ days)**: Monitor rotation.
  - 🟢 **Safe ($> 7$ days)**: Extended shelf life.
- **1-Click Clearance Markdown**: Reduces retail price by 25% or 10% to sell perishables before expiry.

### 4. Weekly Wholesaler Order Consolidation
- Computes aggregate stock requirements for the next 7 days, adjusted for the SASSA cycle.
- Automatically groups orders by supplier:
  - **Devland Cash & Carry**: Maize meal, sugar, pilchards.
  - **Boxer Superstores**: Daily bakery bread, long-life milk, eggs.
  - **Makro Crown Mines**: Sunflower cooking oil, parboiled rice.
- Displays total wholesale spend, projected retail revenue, and gross profit margin percentage.

---

## 🚀 Quick Start Guide

### 1. Web Application (No build required)
Simply open `index.html` in any modern web browser or visit the live deployment:
👉 **[https://bongamanzini.github.io/spaza-smart-inventory/](https://bongamanzini.github.io/spaza-smart-inventory/)**

---

### 2. Python CLI Decision Engine
```bash
# Run standard stockout forecast
python3 spaza_inventory_predictor.py

# Consolidated weekly orders for wholesalers
python3 spaza_inventory_predictor.py --weekly

# Print an 80mm thermal till receipt slip
python3 spaza_inventory_predictor.py --slip

# Generate an official B2B A4 Tax Invoice
python3 spaza_inventory_predictor.py --invoice

# Audit perishable batch expiry dates
python3 spaza_inventory_predictor.py --expiry
```

---

### 3. C++17 Embedded POS Terminal (`terminal-cpp`)
```bash
# Compile using Makefile
cd terminal-cpp
make

# Run the complete end-to-end POS demo
./spaza_terminal --demo

# Process a direct card payment (Amount, Card PAN, PIN)
./spaza_terminal --pay 189.50 4532015112830366 4920

# View weekly wholesaler order sheet
./spaza_terminal --weekly

# Check perishable expiry statuses
./spaza_terminal --expiry

# Output formal Tax Invoice
./spaza_terminal --invoice
```

---

### 4. Android Kotlin Mobile App (`mobile-kotlin`)
```bash
# Open in Android Studio or compile via Gradle wrapper
cd mobile-kotlin
./gradlew assembleDebug
```
The APK will be generated at `mobile-kotlin/app/build/outputs/apk/debug/app-debug.apk`.

---

## 💻 Tech Stack

- **Web Frontend**: HTML5, Vanilla JavaScript (ESNext), Tailwind CSS, Lucide Icons, CSS `@media print`
- **Embedded Engine**: C++17, STL, POSIX, CMake, Make
- **Mobile POS**: Kotlin 1.9+, Android Jetpack Compose, Material 3, Coroutines, StateFlow
- **Data Engine**: Python 3.10+, Pandas, Dataclasses
- **Deployment**: GitHub Pages, Linux POS Terminals (Sunmi / Pax / Yoco), Android APK

---

## 👨‍💻 Author

**Bonga Manzini**  
BSc Informatics – University of South Africa (UNISA)  
Portfolio: [https://bongamanzini.github.io/portfolio/](https://bongamanzini.github.io/portfolio/)  
GitHub: [https://github.com/BongaManzini](https://github.com/BongaManzini)
