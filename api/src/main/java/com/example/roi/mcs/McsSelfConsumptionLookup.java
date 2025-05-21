package com.example.roi.mcs;

import java.io.File;
import java.util.Iterator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Helper class to load the MCS self-consumption JSON lookup file and provide lookup functionality.
 * Usage:
 *   McsSelfConsumptionLookup lookup = new McsSelfConsumptionLookup("mcs_self_consumption.json");
 *   double fraction = lookup.lookup(5200, 3500, 5.0, "Home all day");
 */
public class McsSelfConsumptionLookup {
    private final JsonNode root;

    public McsSelfConsumptionLookup(String jsonPath) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        root = mapper.readTree(new File(jsonPath));
    }

    /**
     * Lookup the self-consumption fraction.
     * @param annualConsumption Annual electricity consumption (kWh)
     * @param pvGenKwh PV generation (kWh)
     * @param batteryKwh Battery size (kWh)
     * @param occupancy Occupancy type (e.g., "Home all day")
     * @return Fraction (double)
     */
    public double lookup(double annualConsumption, double pvGenKwh, double batteryKwh, String occupancy) {
        // Find the correct consumption band
        String bandKey = null;
        Iterator<String> bandKeys = root.fieldNames();
        while (bandKeys.hasNext()) {
            String key = bandKeys.next();
            String[] parts = key.split("-");
            double lo = Double.parseDouble(parts[0]);
            double hi = Double.parseDouble(parts[1]);
            if (annualConsumption >= lo && annualConsumption <= hi) {
                bandKey = key;
                break;
            }
        }
        if (bandKey == null) throw new IllegalArgumentException("No band for " + annualConsumption);
        JsonNode occNode = root.path(bandKey).path(occupancy);
        JsonNode bands = occNode.path("bands");
        for (JsonNode b : bands) {
            double lo = b.path("generation_band_kwh").path("min").asDouble();
            double hi = b.path("generation_band_kwh").path("max").asDouble();
            if (pvGenKwh >= lo && pvGenKwh <= hi) {
                // Find nearest battery size
                JsonNode fracs = b.path("fractions_by_battery_kwh");
                String bestKey = null;
                double bestDelta = Double.MAX_VALUE;
                Iterator<String> it = fracs.fieldNames();
                while (it.hasNext()) {
                    String k = it.next();
                    double bs = Double.parseDouble(k);
                    double d = Math.abs(bs - batteryKwh);
                    if (d < bestDelta) {
                        bestDelta = d;
                        bestKey = k;
                    }
                }
                return fracs.path(bestKey).asDouble();
            }
        }
        throw new IllegalArgumentException("No PV band for " + pvGenKwh);
    }
} 