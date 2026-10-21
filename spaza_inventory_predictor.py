#!/usr/bin/env python3
"""
Spaza-Smart Inventory Predictor
South African Informal Retail Decision-Support Engine

Models consumption velocity and forecasts stockout risk based on
historical SKU turnover and SASSA grant payout schedules.
Author: Bonga Manzini (UNISA BSc Informatics)
"""

import datetime
from dataclasses import dataclass
from typing import List, Dict

@dataclass
class SpazaProduct:
    sku_id: str
    name: str
    category: str
    unit: str
    current_stock: int
    daily_base_sales: float
    sassa_multiplier: float  # Demand surge during SASSA payout week (days 1-6)
    unit_cost_rand: float
    selling_price_rand: float
    supplier_lead_time_days: int

# South African Staple Basket
BASKET: List[SpazaProduct] = [
    SpazaProduct("MZ-125", "White Star Super Maize Meal (12.5kg)", "Staple Grain", "Bags", 8, 4.2, 4.5, 125.0, 155.0, 2),
    SpazaProduct("BR-ALB", "Albany Superior Sliced White Bread (700g)", "Bakery Daily", "Loaves", 15, 35.0, 1.8, 17.5, 21.0, 1),
    SpazaProduct("CO-02L", "Sunfoil Sunflower Cooking Oil (2L)", "Cooking Essentials", "Bottles", 6, 3.0, 3.8, 64.0, 79.9, 2),
    SpazaProduct("SG-02K", "Huletts White Sugar (2kg)", "Dry Pantry", "Packs", 10, 5.0, 3.0, 48.0, 59.9, 2),
    SpazaProduct("RC-10K", "Tastic Parboiled Rice (10kg)", "Staple Grain", "Bags", 5, 2.8, 3.6, 175.0, 215.0, 3),
    SpazaProduct("MK-06P", "First Choice Long Life Milk (6x1L)", "Dairy", "Boxes", 4, 3.5, 2.5, 98.0, 122.0, 2),
    SpazaProduct("PL-400", "Lucky Star Pilchards in Tomato (400g)", "Canned Protein", "Cans", 24, 12.0, 2.8, 22.5, 27.5, 1)
]

def is_sassa_grant_week(dt: datetime.date) -> bool:
    """Returns True if date falls within SASSA grant disbursement cycle (day 1 to 6)."""
    return 1 <= dt.day <= 6

def forecast_stockouts(current_date: datetime.date = None) -> List[Dict]:
    """Projects days of inventory remaining and generates reorder alerts."""
    if current_date is None:
        current_date = datetime.date.today()

    forecast_results = []
    is_surge = is_sassa_grant_week(current_date)

    for prod in BASKET:
        effective_daily_demand = prod.daily_base_sales * (prod.sassa_multiplier if is_surge else 1.0)
        days_remaining = prod.current_stock / effective_daily_demand if effective_daily_demand > 0 else 999.0
        
        # Reorder point: Demand during lead time + 1 day buffer
        reorder_threshold = effective_daily_demand * (prod.supplier_lead_time_days + 1)
        needs_reorder = prod.current_stock <= reorder_threshold

        forecast_results.append({
            "sku": prod.sku_id,
            "name": prod.name,
            "category": prod.category,
            "current_stock": prod.current_stock,
            "unit": prod.unit,
            "daily_velocity": round(effective_daily_demand, 1),
            "days_of_stock": round(days_remaining, 1),
            "needs_urgent_reorder": needs_reorder,
            "recommended_order_units": max(0, int(round((effective_daily_demand * 7) - prod.current_stock))),
            "profit_margin_pct": round(((prod.selling_price_rand - prod.unit_cost_rand) / prod.selling_price_rand) * 100, 1)
        })

    return forecast_results

def print_cli_report():
    today = datetime.date.today()
    surge = is_sassa_grant_week(today)
    print("=" * 75)
    print("🛒 SPAZA-SMART INVENTORY DECISION SUPPORT ENGINE")
    print(f"Date: {today} | SASSA Grant Payout Status: {'🔥 HIGH SURGE ACTIVE' if surge else '🟢 NORMAL DEMAND'}")
    print("=" * 75)
    print(f"{'SKU':<8} | {'Item Name':<32} | {'Stock':<6} | {'Velocity/d':<10} | {'Days Left':<9} | {'Alert'}")
    print("-" * 75)
    
    forecasts = forecast_stockouts(today)
    for f in forecasts:
        alert = "🚨 REORDER NOW" if f["needs_urgent_reorder"] else "✅ OK"
        print(f"{f['sku']:<8} | {f['name'][:32]:<32} | {f['current_stock']:<6} | {f['daily_velocity']:<10} | {f['days_of_stock']:<9} | {alert}")
    print("=" * 75)

if __name__ == "__main__":
    print_cli_report()
