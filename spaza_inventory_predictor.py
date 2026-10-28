#!/usr/bin/env python3
"""
 Spaza-Smart Inventory Predictor & Retail Management System
South African Informal Retail Decision-Support & POS Engine

Features:
- SASSA grant payout cycle consumption velocity forecasting
- Perishable batch tracking & expiry date alert engine
- Wholesaler order consolidation (Makro, Boxer, Devland)
- 80mm/58mm thermal till slip receipt formatter
- Formal B2B Tax Invoice generator
- Weekly shop performance & cash flow analytics

Author: Bonga Manzini (UNISA BSc Informatics)
"""

import sys
import datetime
from dataclasses import dataclass
from typing import List, Dict, Optional

@dataclass
class SpazaProduct:
    sku_id: str
    name: str
    category: str
    unit: str
    current_stock: int
    daily_base_sales: float
    sassa_multiplier: float  # Surge multiplier during SASSA payout week (days 1-6)
    unit_cost_rand: float
    selling_price_rand: float
    supplier_lead_time_days: int
    supplier: str            # Wholesaler name (e.g. Boxer, Makro, Devland)
    batch_no: str            # Batch tracking code
    expiry_date: datetime.date  # Shelf life expiration

# South African Staple Basket with Expiry Dates & Wholesalers
def get_initial_basket() -> List[SpazaProduct]:
    today = datetime.date.today()
    return [
        SpazaProduct(
            "MZ-125", "White Star Super Maize Meal (12.5kg)", "Staple Grain", "Bags",
            8, 4.2, 4.5, 125.0, 155.0, 2, "Devland Cash & Carry", "LOT-MZ881",
            today + datetime.timedelta(days=180)
        ),
        SpazaProduct(
            "BR-ALB", "Albany Superior Sliced White Bread (700g)", "Bakery Daily", "Loaves",
            15, 35.0, 1.8, 17.5, 21.0, 1, "Boxer Superstores", "LOT-BR042",
            today + datetime.timedelta(days=2)  # Perishable!
        ),
        SpazaProduct(
            "CO-02L", "Sunfoil Sunflower Cooking Oil (2L)", "Cooking Essentials", "Bottles",
            6, 3.0, 3.8, 64.0, 79.9, 2, "Makro Crown Mines", "LOT-CO912",
            today + datetime.timedelta(days=240)
        ),
        SpazaProduct(
            "SG-02K", "Huletts White Sugar (2kg)", "Dry Pantry", "Packs",
            10, 5.0, 3.0, 48.0, 59.9, 2, "Devland Cash & Carry", "LOT-SG401",
            today + datetime.timedelta(days=365)
        ),
        SpazaProduct(
            "RC-10K", "Tastic Parboiled Rice (10kg)", "Staple Grain", "Bags",
            5, 2.8, 3.6, 175.0, 215.0, 3, "Makro Crown Mines", "LOT-RC773",
            today + datetime.timedelta(days=300)
        ),
        SpazaProduct(
            "MK-06P", "First Choice Long Life Milk (6x1L)", "Dairy", "Boxes",
            4, 3.5, 2.5, 98.0, 122.0, 2, "Boxer Superstores", "LOT-MK303",
            today + datetime.timedelta(days=45)
        ),
        SpazaProduct(
            "PL-400", "Lucky Star Pilchards in Tomato (400g)", "Canned Protein", "Cans",
            24, 12.0, 2.8, 22.5, 27.5, 1, "Devland Cash & Carry", "LOT-PL109",
            today + datetime.timedelta(days=700)
        ),
        SpazaProduct(
            "EG-30T", "Nulaid Large Eggs Tray (30pk)", "Poultry & Dairy", "Trays",
            7, 6.0, 2.2, 72.0, 89.9, 1, "Boxer Superstores", "LOT-EG550",
            today + datetime.timedelta(days=5)  # Perishable
        ),
    ]

BASKET: List[SpazaProduct] = get_initial_basket()

def is_sassa_grant_week(dt: Optional[datetime.date] = None) -> bool:
    """Returns True if date falls within SASSA grant disbursement cycle (day 1 to 6)."""
    if dt is None:
        dt = datetime.date.today()
    return 1 <= dt.day <= 6

def forecast_stockouts(current_date: Optional[datetime.date] = None) -> List[Dict]:
    """Projects days of inventory remaining, stockout risks, and reorder amounts."""
    if current_date is None:
        current_date = datetime.date.today()

    forecast_results = []
    is_surge = is_sassa_grant_week(current_date)

    for prod in BASKET:
        effective_daily_demand = prod.daily_base_sales * (prod.sassa_multiplier if is_surge else 1.0)
        days_remaining = prod.current_stock / effective_daily_demand if effective_daily_demand > 0 else 999.0
        
        # Reorder threshold: demand during supplier lead time + 1 day safety buffer
        reorder_threshold = effective_daily_demand * (prod.supplier_lead_time_days + 1)
        needs_reorder = prod.current_stock <= reorder_threshold

        # Weekly order requirement (stock to cover next 7 days demand minus current stock)
        weekly_demand = effective_daily_demand * 7
        order_units = max(0, int(round(weekly_demand - prod.current_stock)))

        days_to_expiry = (prod.expiry_date - current_date).days
        if days_to_expiry < 0:
            expiry_status = "EXPIRED"
        elif days_to_expiry <= 3:
            expiry_status = "URGENT_CLEAR"
        elif days_to_expiry <= 7:
            expiry_status = "EXPIRING_SOON"
        else:
            expiry_status = "SAFE"

        forecast_results.append({
            "sku": prod.sku_id,
            "name": prod.name,
            "category": prod.category,
            "supplier": prod.supplier,
            "current_stock": prod.current_stock,
            "unit": prod.unit,
            "unit_cost": prod.unit_cost_rand,
            "selling_price": prod.selling_price_rand,
            "daily_velocity": round(effective_daily_demand, 1),
            "days_of_stock": round(days_remaining, 1),
            "needs_urgent_reorder": needs_reorder,
            "recommended_order_units": order_units,
            "order_cost_rand": round(order_units * prod.unit_cost_rand, 2),
            "profit_margin_pct": round(((prod.selling_price_rand - prod.unit_cost_rand) / prod.selling_price_rand) * 100, 1),
            "batch_no": prod.batch_no,
            "expiry_date": prod.expiry_date.strftime("%Y-%m-%d"),
            "days_to_expiry": days_to_expiry,
            "expiry_status": expiry_status
        })

    return forecast_results

def generate_weekly_order_summary(current_date: Optional[datetime.date] = None) -> Dict:
    """Consolidates weekly stock orders grouped by wholesale supplier."""
    forecasts = forecast_stockouts(current_date)
    suppliers: Dict[str, Dict] = {}
    
    total_units_ordered = 0
    total_order_spend = 0.0
    total_projected_sales = 0.0

    for item in forecasts:
        supplier_name = item["supplier"]
        if supplier_name not in suppliers:
            suppliers[supplier_name] = {
                "supplier": supplier_name,
                "items": [],
                "subtotal_cost": 0.0,
                "total_units": 0
            }
        
        qty = item["recommended_order_units"]
        if qty > 0:
            cost = item["order_cost_rand"]
            sales = qty * item["selling_price"]
            suppliers[supplier_name]["items"].append({
                "sku": item["sku"],
                "name": item["name"],
                "units": qty,
                "unit": item["unit"],
                "cost": cost
            })
            suppliers[supplier_name]["subtotal_cost"] += cost
            suppliers[supplier_name]["total_units"] += qty
            total_units_ordered += qty
            total_order_spend += cost
            total_projected_sales += sales

    projected_gross_profit = total_projected_sales - total_order_spend

    return {
        "date": (current_date or datetime.date.today()).strftime("%Y-%m-%d"),
        "sassa_surge_active": is_sassa_grant_week(current_date),
        "total_units_ordered": total_units_ordered,
        "total_order_spend_rand": round(total_order_spend, 2),
        "total_projected_sales_rand": round(total_projected_sales, 2),
        "projected_gross_profit_rand": round(projected_gross_profit, 2),
        "suppliers": suppliers
    }

def print_thermal_till_slip(
    receipt_no: str = "SPZ-2026-9041",
    customer: str = "Walk-in Township Customer",
    payment_method: str = "CARD - VISA CONTACTLESS",
    card_pan: str = "**** **** **** 4821",
    auth_code: str = "AUTH-829104",
    items_bought: Optional[List[Dict]] = None
) -> str:
    """Generates an authentic 58mm/80mm thermal till slip receipt."""
    if items_bought is None:
        items_bought = [
            {"name": "White Star Maize (12.5kg)", "qty": 1, "price": 155.0},
            {"name": "Sunfoil Cooking Oil (2L)", "qty": 1, "price": 79.9},
            {"name": "Albany Sliced Bread (700g)", "qty": 2, "price": 21.0},
            {"name": "Lucky Star Pilchards (400g)", "qty": 2, "price": 27.5}
        ]

    now = datetime.datetime.now()
    subtotal = sum(i["qty"] * i["price"] for i in items_bought)
    vat = subtotal * 0.15
    total = subtotal

    lines = []
    lines.append("=" * 42)
    lines.append("      SOWETO SMART SPAZA & WHOLESALE     ")
    lines.append("       Vilakazi Street, Orlando West       ")
    lines.append("            Soweto, Gauteng 1804           ")
    lines.append("           VAT REG: 4920192841             ")
    lines.append("            Tel: (011) 935-1829            ")
    lines.append("=" * 42)
    lines.append(f"Date: {now.strftime('%Y-%m-%d %H:%M:%S')}")
    lines.append(f"Slip No: #{receipt_no} | Cashier: Bonga")
    lines.append(f"Customer: {customer[:28]}")
    lines.append("-" * 42)
    lines.append(f"{'Item':<22} {'Qty':>3} {'Price':>7} {'Total':>7}")
    lines.append("-" * 42)

    for itm in items_bought:
        line_total = itm["qty"] * itm["price"]
        item_title = itm["name"][:21]
        lines.append(f"{item_title:<22} {itm['qty']:>3} {itm['price']:>7.2f} {line_total:>7.2f}")

    lines.append("-" * 42)
    lines.append(f"{'SUBTOTAL (EXCL. VAT):':<28} R{round(subtotal - vat, 2):>10.2f}")
    lines.append(f"{'VAT (15% INCL):':<28} R{round(vat, 2):>10.2f}")
    lines.append(f"{'TOTAL PAYABLE:':<28} R{round(total, 2):>10.2f}")
    lines.append("=" * 42)
    lines.append(f"PAYMENT METHOD: {payment_method}")
    if "CARD" in payment_method.upper():
        lines.append(f"Card PAN:       {card_pan}")
        lines.append(f"Card Terminal:  Yoco / Pax EMV South Africa")
        lines.append(f"Auth Approval:  {auth_code} (APPROVED)")
        lines.append(f"STAN / Trace:   928103 / RRN: 8271039812")
    lines.append("-" * 42)
    lines.append("     SIYABONGA! ENKOSI! DANKIE!     ")
    lines.append("  Thank you for supporting Township Retail ")
    lines.append("    Keep slip for returns within 7 days    ")
    lines.append("          [ |||||||||||||||||||| ]         ")
    lines.append("=" * 42)

    slip_text = "\n".join(lines)
    return slip_text

def print_formal_tax_invoice(
    invoice_no: str = "INV-2026-0814",
    customer_name: str = "Orlando East Community Creche",
    customer_addr: str = "Zone 4, Orlando East, Soweto",
    customer_tax: str = "NPO-882-192",
    items: Optional[List[Dict]] = None
) -> str:
    """Generates an official A4 format South African Tax Invoice."""
    if items is None:
        items = [
            {"sku": "MZ-125", "desc": "White Star Super Maize Meal (12.5kg)", "qty": 5, "price": 155.0},
            {"sku": "SG-02K", "desc": "Huletts White Sugar (2kg)", "qty": 10, "price": 59.9},
            {"sku": "MK-06P", "desc": "First Choice Long Life Milk (6x1L)", "qty": 4, "price": 122.0},
            {"sku": "CO-02L", "desc": "Sunfoil Sunflower Cooking Oil (2L)", "qty": 6, "price": 79.9}
        ]

    now = datetime.datetime.now()
    subtotal = sum(i["qty"] * i["price"] for i in items)
    vat = subtotal * 0.15
    total = subtotal

    lines = []
    lines.append("=" * 75)
    lines.append("                         TAX INVOICE")
    lines.append("=" * 75)
    lines.append("ISSUED BY:")
    lines.append("  SPAZA-SMART INFORMAL RETAIL & TRADING (PTY) LTD")
    lines.append("  Registration No: 2024/918274/07 | VAT No: 4920192841")
    lines.append("  1824 Vilakazi Street, Soweto, Johannesburg, 1804")
    lines.append("  Email: orders@spazasmart.co.za | Tel: +27 (011) 935-1829")
    lines.append("-" * 75)
    lines.append(f"INVOICE NUMBER: {invoice_no:<25} DATE: {now.strftime('%d %B %Y')}")
    lines.append(f"PAYMENT TERMS:  7 Days / Instant EFT     DUE DATE: {(now + datetime.timedelta(days=7)).strftime('%d %B %Y')}")
    lines.append("-" * 75)
    lines.append("BILLED TO:")
    lines.append(f"  Client:     {customer_name}")
    lines.append(f"  Address:    {customer_addr}")
    lines.append(f"  Tax/Ref:    {customer_tax}")
    lines.append("-" * 75)
    lines.append(f"{'SKU':<8} {'Item Description':<36} {'Qty':>4} {'Unit (ZAR)':>11} {'Total (ZAR)':>12}")
    lines.append("-" * 75)

    for itm in items:
        row_tot = itm["qty"] * itm["price"]
        lines.append(f"{itm['sku']:<8} {itm['desc'][:35]:<36} {itm['qty']:>4} {itm['price']:>11.2f} {row_tot:>12.2f}")

    lines.append("-" * 75)
    lines.append(f"{'Subtotal (Excl. VAT):':<60} R {subtotal - vat:>10.2f}")
    lines.append(f"{'Value Added Tax (15%):':<60} R {vat:>10.2f}")
    lines.append(f"{'TOTAL AMOUNT DUE:':<60} R {total:>10.2f}")
    lines.append("=" * 75)
    lines.append("BANKING DETAILS FOR INSTANT EFT:")
    lines.append("  Bank:           Capitec Business / First National Bank")
    lines.append("  Account Name:   Spaza-Smart Retail Enterprises")
    lines.append("  Account Number: 1058291048")
    lines.append("  Branch Code:    250655 (FNB) / 470010 (Capitec)")
    lines.append(f"  Reference:      {invoice_no}")
    lines.append("=" * 75)

    return "\n".join(lines)

def print_cli_report():
    today = datetime.date.today()
    surge = is_sassa_grant_week(today)
    print("=" * 82)
    print(" SPAZA-SMART INVENTORY DECISION SUPPORT & RETAIL ENGINE")
    print(f"Calendar Date: {today} | SASSA Cycle: {' HIGH SURGE ACTIVE (Days 1-6)' if surge else ' NORMAL DEMAND'}")
    print("=" * 82)
    print(f"{'SKU':<8} | {'Item Name':<30} | {'Stock':<5} | {'Veloc':<6} | {'Days':<5} | {'Expiry':<10} | {'Alert'}")
    print("-" * 82)
    
    forecasts = forecast_stockouts(today)
    for f in forecasts:
        if f["needs_urgent_reorder"]:
            alert = " REORDER"
        elif f["expiry_status"] == "URGENT_CLEAR":
            alert = "️ EXPIRE SOON"
        else:
            alert = " OK"
        print(f"{f['sku']:<8} | {f['name'][:30]:<30} | {f['current_stock']:<5} | {f['daily_velocity']:<6} | {f['days_of_stock']:<5} | {f['expiry_date']:<10} | {alert}")
    print("=" * 82)

    # Weekly Wholesaler Order Summary
    weekly = generate_weekly_order_summary(today)
    print("\n WEEKLY ORDER FORECAST (CONSOLIDATED WHOLESALER PURCHASES):")
    print(f"Total Units to Order: {weekly['total_units_ordered']} units")
    print(f"Total Wholesaler Cost: R{weekly['total_order_spend_rand']:,.2f}")
    print(f"Projected Sales Value: R{weekly['total_projected_sales_rand']:,.2f}")
    print(f"Projected Gross Profit: R{weekly['projected_gross_profit_rand']:,.2f}")
    print("-" * 82)
    for sup, data in weekly["suppliers"].items():
        if data["total_units"] > 0:
            print(f"   {sup:<26}: {data['total_units']:>3} units | Cost: R{data['subtotal_cost']:>8.2f}")
            for itm in data["items"]:
                print(f"     - {itm['name'][:30]:<30}: {itm['units']} {itm['unit']} (R{itm['cost']:,.2f})")
    print("=" * 82)

if __name__ == "__main__":
    if "--weekly" in sys.argv:
        summary = generate_weekly_order_summary()
        print("\n" + "="*50)
        print(" SPAZA WEEKLY RESTOCK & PERFORMANCE SUMMARY")
        print("="*50)
        for sup, data in summary["suppliers"].items():
            print(f"\nWholesaler: {sup} (Total: R{data['subtotal_cost']:,.2f})")
            for itm in data["items"]:
                print(f"  • {itm['units']}x {itm['name']} (R{itm['cost']:,.2f})")
        print(f"\nTotal Order Spend: R{summary['total_order_spend_rand']:,.2f}")
        print(f"Projected Profit:  R{summary['projected_gross_profit_rand']:,.2f}")
    elif "--slip" in sys.argv:
        print(print_thermal_till_slip())
    elif "--invoice" in sys.argv:
        print(print_formal_tax_invoice())
    elif "--expiry" in sys.argv:
        print("\n️ PERISHABLE & EXPIRY TRACKING AUDIT:")
        for itm in forecast_stockouts():
            status_symbol = "" if itm["days_to_expiry"] <= 3 else ""
            print(f"{status_symbol} {itm['name'][:32]:<32} | Batch: {itm['batch_no']} | Expires: {itm['expiry_date']} ({itm['days_to_expiry']} days left)")
    else:
        print_cli_report()
