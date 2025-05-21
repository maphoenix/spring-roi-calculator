# MCS Self-Consumption Tables

## Overview
The MCS (Microgeneration Certification Scheme) self-consumption tables provide standardized data for estimating the self-consumption fraction of solar PV systems with battery storage. These tables are used to determine what percentage of generated solar energy will be consumed on-site based on various factors.

## Purpose
The tables help calculate realistic self-consumption estimates for solar PV installations by considering:
- Household occupancy patterns
- Annual electricity consumption
- Solar PV system size (annual generation)
- Battery storage capacity

## Data Structure
The data is organized in a hierarchical JSON format:

```json
{
  "occupancy_type": {
    "consumption_ranges": [
      {
        "occupancy": "Home all day",
        "consumption": "Annual consumption: 1,500 kWh to 1,999 kWh",
        "battery_sizes": ["0", "1.1", "2.1", "3.1", "4.1"],
        "bands": [
          {
            "pv_generation_range": "300-599",
            "pv_min": 300,
            "pv_max": 599,
            "batteries": [
              {
                "size": "0",
                "pv_generated_percentage": 0.51
              }
            ]
          }
        ]
      }
    ]
  }
}
```

### Key Components

1. **Occupancy Types**
   - Different household occupancy patterns (e.g., "Home all day", "Out during day")
   - Reflects typical usage patterns affecting self-consumption

2. **Consumption Ranges**
   - Annual electricity consumption bands (e.g., 1,500-1,999 kWh)
   - Represents typical household electricity usage

3. **PV Generation Bands**
   - Ranges of annual PV generation (e.g., 300-599 kWh)
   - Based on system size and expected generation

4. **Battery Sizes**
   - Available battery storage capacities (in kWh)
   - Includes 0 kWh for systems without battery storage

5. **Self-Consumption Fractions**
   - Percentage of generated electricity consumed on-site
   - Values between 0 and 1 (e.g., 0.51 = 51% self-consumption)

## Usage Constraints

### Valid Ranges
- Annual Consumption: 0 to 20,000 kWh
- PV Generation: 0 to 10,000 kWh
- Battery Size: 0 to 50 kWh

### Interpolation
The system can interpolate between values to provide estimates for scenarios that don't exactly match the table entries. This uses a weighted similarity approach considering:
- Occupancy type (40% weight)
- Annual consumption (30% weight)
- PV generation (20% weight)
- Battery size (10% weight)

## Example Usage

```java
McsLookup lookup = new McsLookup("mcs_self_consumption.json");

// Find exact match
double fraction = lookup.lookup(
    "Home all day",    // occupancy type
    1750,             // annual consumption (kWh)
    400,              // PV generation (kWh)
    2.1               // battery size (kWh)
);

// Find closest match with similarity score
MatchResult result = lookup.findClosestMatch(
    "Home all day",
    1750,
    400,
    2.1
);
```

## Data Sources
The MCS tables are based on statistical analysis and modeling of real-world solar PV installations. The data is maintained and updated by the Microgeneration Certification Scheme to reflect current technology performance and usage patterns.

## References
- [MCS Website](https://mcscertified.com/)
- [MCS PV Guide](https://mcscertified.com/standards-tools-library/)
- [Solar PV Standard MIS 3002](https://mcscertified.com/standards-tools-library/) 