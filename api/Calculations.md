# Calculations

##Explanation of the Numbers Used

##1. Kwh per kW of solar per year
There are typically 850 kWh per 1 kW of solar per year
When you install solar panels in the UK (or a similar climate), a very typical rule of thumb is:

1 kW of solar panels produces about 850 kWh per year

It's based on average sunlight hours (also called "solar irradiance") across the UK.


Solar Array Size	Annual Solar Output (approx)
1 kW	850 kWh
4 kW	3400 kWh

So if you have a 4 kW solar array, expected generation = 4 × 850 = 3400 kWh/year

##2. Usable Battery Capacity
Your battery size is reduced by 10% because:

Fox (and most batteries) reserve 10% to protect battery life.

So:
Usable Capacity = Battery Size × 90%

Example:

17.5 kWh battery → usable ~15.75 kWh

##3. Daily Shifting Calculation
You can charge your battery every night at cheap off-peak rates and discharge it during the day to avoid expensive peak rates.

The calculation assumes:

Shiftable energy per day = usable battery size (e.g., 15.75 kWh)

Over a year = 15.75 × 365 = ~5748.75 kWh/year
But we cap it at your house usage if house demand is lower.

##4. Battery Degradation
Over time, the battery degrades.

We assumed 15% degradation from 100% to 85% average usable capacity over the system life.

This corrects the savings calculation.

##5. Savings Calculation for Battery
Battery savings = (amount shifted) × (peak price – off-peak price) × 85% (degradation correction)

Example for Intelligent Octopus Go:

Peak price: 27.71p

Off-peak price: 7.5p

Difference: 20.21p

Then:

Savings = shiftable kWh × 0.2021 × 0.85

##6. Solar Panel Contribution
Solar used on-site: ~50% of solar generation

Solar exported: ~50% of solar generation

But export is scaled down in practice, especially winter.

##Savings from solar:

Self-used solar valued at peak price (saving)

Exported solar paid at export rate (income)

##Full Formula Summary
Total Annual Savings = 
  (Shiftable kWh × (Peak Rate – Offpeak Rate) × 0.85)
+ (Solar Used × Peak Rate)
+ (Solar Exported × Export Rate)
Where:

Solar Used = 50% of Solar Generation

Solar Exported = 50% × 60% = 30% effective (because of seasonal/winter correction)

##Quick Recap of Constants

Factor	Value	Notes
Solar yield (kW ➔ kWh)	850 kWh/kW/yr	Typical UK average
Battery usable fraction	90%	10% reserve enforced
Battery degradation	85% effective	Over lifetime
Solar self-use rate	50%	Typical home usage split
Solar export effectiveness	~30%	Conservative winter correction