# 📱 Spaza-Smart Mobile POS & Inventory (Android Kotlin)

An enterprise-ready, township-tailored Android Mobile Point-of-Sale (POS) and inventory decision-support system written in **Modern Kotlin** with **Jetpack Compose** and **Material 3**.

---

## 🚀 Key Features

1. **Card & Contactless Payments**:
   - **NFC Tap-to-Phone**: Simulates contactless card acceptance (Visa, Mastercard, SASSA Postbank Gold Card).
   - **EMV Chip & PIN**: Card verification with PIN entry modal.
   - **Luhn Algorithm Checksum**: Automatic validation of 16-digit card Primary Account Numbers (PAN).
   - **BankservAfrica & PASA Switching**: Simulated South African interbank authorization producing standard Approval Codes, Retrieval Reference Numbers (RRN), and System Trace Audit Numbers (STAN).
   - **PCI-DSS Masking**: Card numbers are masked as `**** **** **** 1234` on slips and logs.

2. **Thermal Receipt Slip & Tax Invoice Generator**:
   - Generates authentic 58mm / 80mm ESC/POS thermal receipts.
   - Generates full A4 formal South African Tax Invoices with VAT registration numbers and Capitec / FNB banking details for Instant EFT.
   - Direct Bluetooth print preview for mobile thermal printers (Sunmi, Pax, Yoco, Posiflex).

3. **Perishable Expiry Date Monitoring**:
   - Tracks batch numbers (`LOT-MZ881`, `LOT-BR042`, etc.) and days until expiry.
   - Alerts for critical items expiring in `< 3 days` (e.g. Albany Bread).
   - **1-Click Clearance Markdown**: Suggests and applies automatic 25% or 10% clearance discounts to prevent township inventory wastage.

4. **Weekly Wholesaler Order Consolidation**:
   - Real-time aggregation of inventory requirements for the next 7 days based on the **SASSA grant payout multiplier**.
   - Grouped by regional cash & carry wholesalers:
     - **Boxer Superstores** (Daily bakery, long-life milk, eggs)
     - **Devland Cash & Carry** (Maize meal, sugar, pilchards)
     - **Makro Crown Mines** (Sunflower oil, parboiled rice)
   - Displays total units ordered, wholesale spend, projected revenue, and estimated gross profit margin.

---

## 🛠️ Tech Stack & Architecture

- **Language**: Kotlin 1.9+
- **UI Framework**: Android Jetpack Compose (BOM 2024.06.00), Material 3
- **Architecture**: Clean Architecture + MVVM + StateFlow / Coroutines
- **Target SDK**: Android 14 (API 34) | **Min SDK**: Android 7.0 (API 24)
- **Permissions**: `NFC`, `BLUETOOTH`, `BLUETOOTH_CONNECT`, `INTERNET`

---

## 📦 How to Build & Run

### In Android Studio:
1. Open Android Studio -> **File > Open...** -> Select the `mobile-kotlin` directory.
2. Allow Gradle sync to complete.
3. Select an emulator or physical Android device with NFC enabled.
4. Click **Run > Run 'app'** (`Shift + F10`).

### Via Command Line:
```bash
cd mobile-kotlin
./gradlew assembleDebug
```
The APK will be generated at:
`mobile-kotlin/app/build/outputs/apk/debug/app-debug.apk`
