# Synthetic Self-Consumption Dataset: Interpolated Occupancy Model

## Overview

This dataset estimates solar self-consumption (%) based on:

- PV generation (`PV_kWh`) from **0 to 10,000 kWh** (in 500 kWh steps)
- Battery size (`Battery_kWh`) from **0 to 20 kWh** (in 1.0 kWh steps)
- Annual electricity consumption (`Consumption_kWh`) using known usage bands from parsed MCS data
- Occupancy level (`OccupancyValue`) ranging from **0.5 to 5.0** in 0.5 day increments (representing weekday occupancy)

## Methodology

The dataset was generated using the following approach:

1. **Base Data**: Start with known self-consumption values from structured MCS JSON, categorized by occupancy ID and consumption band.

2. **Extrapolation for PV and Battery**:  
   Use a diminishing returns model to extend PV generation and battery capacity beyond the known data range:

   ```
   SelfConsumption = 100 - (100 - base) * exp(-a * PV) * exp(-b * Battery)
   ```
   where `a = 0.0003` and `b = 0.15`.

3. **Interpolated Occupancy**:  
   For occupancy values between known categories (e.g., 1.0 = out during day, 3.0 = partial occupancy, 5.0 = home all day), apply smooth interpolation:

   ```
   InterpolatedSC = baseSC * exp(-c * abs(occupancy_value - known_occupancy))
   ```
   using `c = 0.1` as the decay factor.

4. **Clamping**:  
   All final self-consumption values are clamped to the range [0, 100].

## Output Columns

- `OccupancyValue`: Days at home per week (0.5 to 5.0)
- `Consumption_kWh`: Annual electricity usage (from MCS data)
- `PV_kWh`: Midpoint of PV generation range
- `Battery_kWh`: Battery capacity (kWh)
- `SelfConsumption`: Estimated percentage of solar used on-site