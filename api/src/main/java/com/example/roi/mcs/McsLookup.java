import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.util.Iterator;

public class McsLookup {
    public static class Entry {
        public final double minPv, maxPv;
        public final double batterySize;
        public final double fraction;
        public Entry(double minPv, double maxPv, double batterySize, double fraction) {
            this.minPv = minPv;
            this.maxPv = maxPv;
            this.batterySize = batterySize;
            this.fraction = fraction;
        }
    }

    private final JsonNode root;

    public McsLookup(String jsonPath) throws Exception {
        ObjectMapper M = new ObjectMapper();
        root = M.readTree(new File(jsonPath));
    }

    /**
     * Lookup self-consumption fraction.
     *
     * @param annualConsumption  e.g. 5200
     * @param pvGenKwh           actual annual PV generation (kWh)
     * @param batteryKwh         usable battery size (kWh)
     * @param occupancyKey       e.g. "Occupancy: Home all day"
     */
    public double lookup(double annualConsumption,
                         double pvGenKwh,
                         double batteryKwh,
                         String occupancyKey) {

        Iterator<String> bands = root.fieldNames();
        String matchBand = null;
        while (bands.hasNext()) {
            String band = bands.next();
            String[] parts = band.split("-");
            double lo = Double.parseDouble(parts[0]);
            double hi = Double.parseDouble(parts[1]);
            if (annualConsumption >= lo && annualConsumption <= hi) {
                matchBand = band;
                break;
            }
        }
        if (matchBand == null) throw new IllegalArgumentException("No consumption band for " + annualConsumption);

        JsonNode occNode = root.path(matchBand).path(occupancyKey).path("bands");
        if (!occNode.isArray()) throw new IllegalArgumentException("Occupancy not found: " + occupancyKey);

        for (JsonNode band : occNode) {
            double lo = band.path("generation_band_kwh").path("min").asDouble();
            double hi = band.path("generation_band_kwh").path("max").asDouble();
            if (pvGenKwh >= lo && pvGenKwh <= hi) {
                JsonNode fracs = band.path("fractions_by_battery_kwh");
                double bestDelta = Double.MAX_VALUE;
                double chosenFrac = 0;
                Iterator<String> keys = fracs.fieldNames();
                while (keys.hasNext()) {
                    String k = keys.next();
                    double bs = Double.parseDouble(k);
                    double delta = Math.abs(bs - batteryKwh);
                    if (delta < bestDelta) {
                        bestDelta = delta;
                        chosenFrac = fracs.path(k).asDouble();
                    }
                }
                return chosenFrac;
            }
        }

        throw new IllegalArgumentException("No PV band match for " + pvGenKwh);
    }
}
