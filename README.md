# 🛒 Spaza-Smart Inventory Predictor

> **Live Application**: [https://bongamanzini.github.io/spaza-smart-inventory/](https://bongamanzini.github.io/spaza-smart-inventory/)

A decision-support system and inventory forecasting tool engineered for township Spaza shops and micro-retailers in South Africa. Predicts inventory stockouts by correlating staple grocery turnover with the monthly South African Social Security Agency (SASSA) grant payout cycle.

---

## 🎯 The Informatics & Informal Economy Angle

- **The Problem**: Informal Spaza shop owners lose up to **18% of their gross monthly earnings** because they run out of basic staple goods (maize meal, cooking oil, bread, sugar) during the first 5 days of the month when millions of SASSA grant beneficiaries receive and spend their funds.
- **The Informatics Solution**:
  - Models daily SKU sales velocity using a **SASSA multiplier** (surges of 300% to 450% on basic foodstuffs during grant disbursement weeks).
  - Calculates lead times from regional Cash & Carry wholesalers (Makro, Boxer, Devland).
  - Generates automated restock triggers before weekend stockouts occur.

---

## 🛠️ Features

- **Dual-Interface Architecture**:
  1. **Python CLI Engine (`spaza_inventory_predictor.py`)**: Data pipeline modeling consumption velocity and safety stock margins.
  2. **Interactive Web Dashboard (`index.html`)**: Real-time simulation toggle comparing "SASSA Grant Payout Week" vs "Mid-Month Normal".
- **SKU Staple Basket**: Maize Meal (12.5kg), Sliced White Bread, Sunflower Oil (2L), Sugar (2kg), Parboiled Rice (10kg), Long Life Milk (6x1L), Pilchards (400g).

---

## 💻 Tech Stack

- **Data Engine**: Python 3, Pandas
- **Frontend**: HTML5, Vanilla JavaScript (ESNext), Tailwind CSS
- **Deployment**: GitHub Pages (Web) & Python CLI

---

## 🚀 Quick Start (Python CLI)

```bash
# Run prediction engine
python3 spaza_inventory_predictor.py
```

---

## 👨‍💻 Author

**Bonga Manzini**  
BSc Informatics – University of South Africa (UNISA)  
Portfolio: [https://bongamanzini.github.io/portfolio/](https://bongamanzini.github.io/portfolio/)
