# EcoSmart City Bangladesh - Quick Tips Guide

এই guide টা Bangladesh context এ project demo, viva, আর daily usage আরও smooth করার জন্য।

## 1. Data Entry Tips
- প্রতিটি entity এর `Entity ID` unique রাখো।
- `Energy Usage`, `Resident Count`, `Pollution Level` valid numeric value দাও।
- Duplicate ID বা invalid input দিলে add/update fail করবে।

## 2. Dashboard Read Tips
- `Total Entities`: মোট কত entity track করা হচ্ছে।
- `Total CO2 Emission`: overall carbon impact snapshot (kg CO2)।
- `Alerts`: high-risk/overuse conditions এর count।
- `Eco Score`: sustainability health indicator (0-100)।
- `Top Green`: সবচেয়ে sustainable entity।

## 3. Carbon Impact কীভাবে Measure হয়
- Residential entity এর জন্য:
	Carbon Impact = Energy Usage x 0.45
- Industrial entity এর জন্য:
	Carbon Impact = Pollution Level x 2.5
- Dashboard এর `Total CO2 Emission`:
	সব entity এর carbon impact যোগফল

## 4. Project Calculation Rules (Core Logic)
- Residential Bill:
	Bill (BDT) = Energy Usage x 12.5
- Industrial Penalty:
	Pollution Level > 100 হলে Penalty (BDT) = 5000, নাহলে 0
- Residential Eco Score:
	Score = 100 - (Energy x 0.05) - (Resident Count x 1.5)
- Industrial Eco Score:
	Score = 100 - (Energy x 0.04) - (Pollution x 0.25)
- Eco Score clamp:
	Final score সবসময় 0 থেকে 100 এর মধ্যে রাখা হয়
- Alert trigger summary:
	Residential: energy overuse (energy > 500) হলে alert
	Industrial: pollution > 100 বা energy > 900 হলে alert

## 5. Smart Workflow (Recommended)
1. Add entities one by one.
2. Use Search + Filter to inspect specific groups.
3. Fix ALERT entities via edit.
4. Save (`Ctrl+S`) frequently.
5. Export reports/backup before closing.

## 6. Keyboard Shortcuts
- `Ctrl+S` -> Save data
- `Ctrl+E` -> Export CSV
- `Ctrl+J` -> Export JSON backup
- `Ctrl+I` -> Import JSON backup
- `Ctrl+D` -> Delete selected row
- `Ctrl+R` -> Sustainability report
- `Esc` -> Clear form
- `Enter` -> Submit form (default action)

## 7. Demo / Presentation Tips
- আগে থেকে Dhaka/Chattogram style 2-3 Residential + 2-3 Industrial data add করে রাখো।
- একবার filter করে শুধু `ALERT` show করো।
- তারপর edit করে improvement দেখাও (live impact on dashboard)।
- শেষে CSV/JSON export করে backup/reporting capability দেখাও।

## 8. Safety Tips
- Import করার আগে JSON backup export করে রাখো।
- Large data update এর আগে quick backup নেওয়া best practice।
- Project folder এর `data` directory নিয়মিত check করো।

## 9. Suggested Next Small Upgrades
- Chart-এ percentage labels দেখানো
- Alert severity levels (Low/Medium/High)
- Last updated timestamp in dashboard
- Mini activity log panel
