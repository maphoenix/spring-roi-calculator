package com.example.roi.mcs;

import java.io.File;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * McsLookup provides functionality to find self-consumption percentages from MCS lookup tables.
 * It supports both exact matches and finding the closest matching data point based on multiple criteria.
 * 
 * The class handles JSON data structured with:
 * - Occupancy types (e.g., "home_all_day")
 * - Consumption ranges (e.g., "1,500 kWh to 1,999 kWh")
 * - PV generation bands (e.g., "300-599" kWh)
 * - Battery sizes and their corresponding self-consumption percentages
 *
 * The matching algorithm prioritizes criteria in the following order:
 * 1. Occupancy type (40% weight)
 * 2. Annual consumption (30% weight)
 * 3. PV generation (20% weight)
 * 4. Battery size (10% weight)
 *
 * Input Validation Rules:
 * - Occupancy type: Must be non-null and non-empty
 * - Annual consumption: Must be between 0 and 20,000 kWh
 * - PV generation: Must be between 0 and 10,000 kWh
 * - Battery size: Must be between 0 and 50 kWh
 *
 * Exception Handling:
 * - {@link InvalidParameterException}: Thrown when input parameters are outside their allowed ranges
 * - {@link IllegalArgumentException}: Thrown when no valid matches can be found in the data
 *
 * Example Usage:
 * <pre>
 * McsLookup lookup = new McsLookup("path/to/mcs_data.json");
 * 
 * // Find exact match
 * double percentage = lookup.lookup(
 *     "Home all day",    // occupancy type
 *     1750,             // annual consumption (kWh)
 *     400,              // PV generation (kWh)
 *     2.1               // battery size (kWh)
 * );
 * 
 * // Find closest match with similarity score
 * MatchResult result = lookup.findClosestMatch(
 *     "Home all day",    // occupancy type
 *     1750,             // annual consumption (kWh)
 *     400,              // PV generation (kWh)
 *     2.1               // battery size (kWh)
 * );
 * System.out.println("Matched percentage: " + result.percentage);
 * System.out.println("Similarity score: " + result.similarity);
 * </pre>
 *
 * @see InvalidParameterException
 */
public class McsLookup {
    
    /**
     * Represents a single entry in the lookup table with its range parameters and percentage value.
     */
    public static class Entry {
        public final double minPv, maxPv;
        public final double batterySize;
        public final double percentage;
        
        public Entry(double minPv, double maxPv, double batterySize, double percentage) {
            this.minPv = minPv;
            this.maxPv = maxPv;
            this.batterySize = batterySize;
            this.percentage = percentage;
        }
    }

    /**
     * Contains the results of a closest match search, including all matched parameters
     * and a similarity score indicating the quality of the match.
     */
    public static class MatchResult {
        /** The matched occupancy type (e.g., "Home all day") */
        public final String matchedOccupancy;
        /** The matched consumption range (e.g., "Annual consumption: 1,500 kWh to 1,999 kWh") */
        public final String matchedConsumption;
        /** The matched PV generation range (e.g., "300-599") */
        public final String matchedPvRange;
        /** The matched battery size in kWh */
        public final double matchedBatterySize;
        /** The self-consumption percentage for this combination */
        public final double percentage;
        /** Similarity score (0-1) indicating how close the match is to the requested values */
        public final double similarity;

        public MatchResult(String matchedOccupancy, String matchedConsumption, 
                         String matchedPvRange, double matchedBatterySize, 
                         double percentage, double similarity) {
            this.matchedOccupancy = matchedOccupancy;
            this.matchedConsumption = matchedConsumption;
            this.matchedPvRange = matchedPvRange;
            this.matchedBatterySize = matchedBatterySize;
            this.percentage = percentage;
            this.similarity = similarity;
        }
    }

    /** The root node of the JSON data structure */
    private final JsonNode root;
    
    /** Minimum allowed values for parameters */
    private static final double MIN_CONSUMPTION = 0.0;
    private static final double MIN_PV_GENERATION = 0.0;
    private static final double MIN_BATTERY_SIZE = 0.0;
    
    /** Maximum allowed values for parameters (can be adjusted based on requirements) */
    private static final double MAX_CONSUMPTION = 20000.0;  // 20,000 kWh
    private static final double MAX_PV_GENERATION = 10000.0; // 10,000 kWh
    private static final double MAX_BATTERY_SIZE = 50.0;    // 50 kWh

    /**
     * Creates a new McsLookup instance by loading data from a JSON file.
     *
     * @param jsonPath Path to the JSON file containing the lookup table data
     * @throws Exception if the file cannot be read or parsed
     */
    public McsLookup(String jsonPath) throws Exception {
        ObjectMapper M = new ObjectMapper();
        root = M.readTree(new File(jsonPath));
    }

    /**
     * Normalizes an occupancy key to match the format used in the JSON data.
     * Converts "Occupancy: Home all day" to "home_all_day" format.
     *
     * @param occupancyKey The occupancy key to normalize
     * @return The normalized occupancy key
     */
    private String normalizeOccupancyKey(String occupancyKey) {
        if (occupancyKey.startsWith("Occupancy: ")) {
            occupancyKey = occupancyKey.substring("Occupancy: ".length());
        }
        return occupancyKey.toLowerCase().replace(" ", "_");
    }

    private int getOccupancyId(String occupancyType) {
        switch (normalizeOccupancyKey(occupancyType)) {
            case "home_all_day":
                return 1;
            case "out_during_day":
                return 2;
            case "in_half_day":
                return 3;
            case "hybrid":
                return 4;
            default:
                throw new IllegalArgumentException("Unknown occupancy type: " + occupancyType);
        }
    }

    /**
     * Calculates string similarity using Levenshtein distance.
     * Returns a normalized similarity score between 0 (completely different) and 1 (identical).
     *
     * @param s1 First string to compare
     * @param s2 Second string to compare
     * @return Similarity score between 0 and 1
     */
    private double calculateStringSimilarity(String s1, String s2) {
        s1 = s1.toLowerCase();
        s2 = s2.toLowerCase();
        int maxLength = Math.max(s1.length(), s2.length());
        if (maxLength == 0) return 1.0;
        return 1.0 - ((double) levenshteinDistance(s1, s2) / maxLength);
    }

    /**
     * Calculates the Levenshtein distance between two strings.
     * This is the minimum number of single-character edits required to change one string into the other.
     */
    private int levenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];
        
        for (int i = 0; i <= s1.length(); i++) {
            for (int j = 0; j <= s2.length(); j++) {
                if (i == 0) {
                    dp[i][j] = j;
                } else if (j == 0) {
                    dp[i][j] = i;
                } else {
                    dp[i][j] = Math.min(
                        Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                        dp[i - 1][j - 1] + (s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1)
                    );
                }
            }
        }
        return dp[s1.length()][s2.length()];
    }

    /**
     * Parses a consumption range string into min and max values.
     * Example: "Annual consumption: 1,500 kWh to 1,999 kWh" -> [1500.0, 1999.0]
     */
    private double[] parseConsumptionRange(String consumption) {
        String[] parts = consumption.substring("Annual consumption: ".length())
                                  .replace(" kWh", "")
                                  .replace(",", "")
                                  .split(" to ");
        return new double[] { Double.parseDouble(parts[0]), Double.parseDouble(parts[1]) };
    }

    /**
     * Calculates the similarity between a PV generation value and a range.
     * Returns 1.0 for exact matches (within range) and decreases based on distance to range boundaries.
     *
     * @param pvGenKwh The PV generation value to check
     * @param pvMin The minimum value of the range
     * @param pvMax The maximum value of the range
     * @return Similarity score between 0 and 1
     */
    private double calculatePvSimilarity(double pvGenKwh, double pvMin, double pvMax) {
        // If the value is within the range, it's a perfect match
        if (pvGenKwh >= pvMin && pvGenKwh <= pvMax) {
            return 1.0;
        }
        
        // Calculate distance to range boundaries
        double distanceToMin = Math.abs(pvGenKwh - pvMin);
        double distanceToMax = Math.abs(pvGenKwh - pvMax);
        double closestDistance = Math.min(distanceToMin, distanceToMax);
        
        // Calculate range size for normalization
        double rangeSize = pvMax - pvMin;
        
        // Normalize the distance to get similarity (1 - normalized distance)
        return Math.max(0, 1.0 - (closestDistance / rangeSize));
    }

    /**
     * Validates input parameters against allowed ranges.
     * 
     * @throws InvalidParameterException if any parameter is outside its allowed range
     */
    private void validateInputParameters(String occupancyType, 
                                       double annualConsumption,
                                       double pvGenKwh,
                                       double batteryKwh) {
        if (occupancyType == null || occupancyType.trim().isEmpty()) {
            throw new InvalidParameterException("Occupancy type cannot be null or empty");
        }

        if (annualConsumption < MIN_CONSUMPTION || annualConsumption > MAX_CONSUMPTION) {
            throw new InvalidParameterException(
                String.format("Annual consumption must be between %.1f and %.1f kWh, got: %.1f",
                    MIN_CONSUMPTION, MAX_CONSUMPTION, annualConsumption)
            );
        }

        if (pvGenKwh < MIN_PV_GENERATION || pvGenKwh > MAX_PV_GENERATION) {
            throw new InvalidParameterException(
                String.format("PV generation must be between %.1f and %.1f kWh, got: %.1f",
                    MIN_PV_GENERATION, MAX_PV_GENERATION, pvGenKwh)
            );
        }

        if (batteryKwh < MIN_BATTERY_SIZE || batteryKwh > MAX_BATTERY_SIZE) {
            throw new InvalidParameterException(
                String.format("Battery size must be between %.1f and %.1f kWh, got: %.1f",
                    MIN_BATTERY_SIZE, MAX_BATTERY_SIZE, batteryKwh)
            );
        }
    }

    /**
     * Finds the closest matching data point based on multiple criteria.
     * The matching algorithm uses weighted similarity scores for each criterion:
     * - Occupancy type: 40% (using string similarity)
     * - Annual consumption: 30% (using range proximity)
     * - PV generation: 20% (using range proximity)
     * - Battery size: 10% (using value proximity)
     *
     * Input parameters are validated against the following rules:
     * - occupancyType: Must be non-null and non-empty string
     * - annualConsumption: Must be between 0 and 20,000 kWh
     * - pvGenKwh: Must be between 0 and 10,000 kWh
     * - batteryKwh: Must be between 0 and 50 kWh
     *
     * The method returns a MatchResult object containing:
     * - matchedOccupancy: The closest matching occupancy type
     * - matchedConsumption: The matched consumption range
     * - matchedPvRange: The matched PV generation range
     * - matchedBatterySize: The closest matching battery size
     * - percentage: The self-consumption percentage for this combination
     * - similarity: A score between 0 and 1 indicating match quality
     *
     * @param occupancyType The occupancy type to match (e.g., "Home all day")
     * @param annualConsumption Annual consumption in kWh (0-20,000)
     * @param pvGenKwh Annual PV generation in kWh (0-10,000)
     * @param batteryKwh Battery size in kWh (0-50)
     * @return MatchResult containing the best matching data point and similarity score
     * @throws InvalidParameterException if any input parameter is outside its allowed range
     * @throws IllegalArgumentException if no valid matches can be found in the data
     */
    public MatchResult findClosestMatch(String occupancyType, 
                                      double annualConsumption,
                                      double pvGenKwh,
                                      double batteryKwh) {
        // Validate input parameters
        validateInputParameters(occupancyType, annualConsumption, pvGenKwh, batteryKwh);

        double bestSimilarity = -1;
        MatchResult bestMatch = null;

        // Iterate through all occupancy types (1, 2, 3)
        for (int id = 1; id <= 3; id++) {
            JsonNode occupancyNode = root.path(String.valueOf(id));
            if (occupancyNode.isMissingNode()) continue;

            JsonNode ranges = occupancyNode.path("consumption_ranges");
            if (!ranges.isArray()) continue;

            for (JsonNode range : ranges) {
                // Get occupancy details
                JsonNode occ = range.path("occupancy");
                String currentOccupancy = occ.path("type").asText();
                
                // Calculate string similarity for occupancy type
                double occupancySimilarity = calculateStringSimilarity(
                    currentOccupancy,
                    occupancyType
                );

                // Get consumption range
                JsonNode consumption = range.path("annual_consumption");
                double minConsumption = consumption.path("min").asDouble();
                double maxConsumption = consumption.path("max").asDouble();
                
                // Calculate consumption similarity based on distance to range boundaries
                double consumptionSimilarity = 1.0 - Math.min(
                    Math.abs(annualConsumption - minConsumption),
                    Math.abs(annualConsumption - maxConsumption)
                ) / Math.max(maxConsumption, annualConsumption);

                JsonNode bands = range.path("bands");
                for (JsonNode band : bands) {
                    double pvMin = band.path("pv_min").asDouble();
                    double pvMax = band.path("pv_max").asDouble();
                    
                    // Calculate PV generation similarity
                    double pvSimilarity = calculatePvSimilarity(pvGenKwh, pvMin, pvMax);

                    JsonNode batteries = band.path("batteries");
                    for (JsonNode battery : batteries) {
                        double batterySize = Double.parseDouble(battery.path("size").asText());
                        double percentage = battery.path("pv_generated_percentage").asDouble();
                        
                        // Calculate similarity for battery size
                        double batterySimilarity = 1.0 - Math.min(
                            Math.abs(batteryKwh - batterySize),
                            Math.abs(batteryKwh - batterySize) / Math.max(batterySize, batteryKwh)
                        );
                        
                        // Calculate overall similarity using weighted components
                        double totalSimilarity = (
                            occupancySimilarity * 0.4 +    // 40% weight for occupancy
                            consumptionSimilarity * 0.3 +  // 30% weight for consumption
                            pvSimilarity * 0.2 +           // 20% weight for PV generation
                            batterySimilarity * 0.1        // 10% weight for battery size
                        );
                        
                        if (totalSimilarity > bestSimilarity) {
                            bestSimilarity = totalSimilarity;
                            bestMatch = new MatchResult(
                                currentOccupancy,
                                String.format("Annual consumption: %,d kWh to %,d kWh", (int)minConsumption, (int)maxConsumption),
                                band.path("pv_generation_range").asText(),
                                batterySize,
                                percentage,
                                totalSimilarity
                            );
                        }
                    }
                }
            }
        }

        if (bestMatch == null) {
            throw new IllegalArgumentException("No valid matches found in the data");
        }

        return bestMatch;
    }

    /**
     * Lookup self-consumption percentage.
     *
     * @param annualConsumption  e.g. 5200
     * @param pvGenKwh           actual annual PV generation (kWh)
     * @param batteryKwh         usable battery size (kWh)
     * @param occupancyKey       e.g. "Occupancy: Home all day"
     * @return The self-consumption percentage (0-100)
     */
    public double lookup(double annualConsumption,
                        double pvGenKwh,
                        double batteryKwh,
                        String occupancyKey) {

        int occupancyId = getOccupancyId(occupancyKey);
        JsonNode occupancyNode = root.path(String.valueOf(occupancyId));
        
        if (occupancyNode.isMissingNode()) {
            throw new IllegalArgumentException("Occupancy not found: " + occupancyKey);
        }

        JsonNode ranges = occupancyNode.path("consumption_ranges");
        if (!ranges.isArray()) {
            throw new IllegalArgumentException("Invalid format: consumption_ranges not found for " + occupancyKey);
        }

        // Find matching consumption range
        JsonNode matchingRange = null;
        for (JsonNode range : ranges) {
            JsonNode consumption = range.path("annual_consumption");
            double min = consumption.path("min").asDouble();
            double max = consumption.path("max").asDouble();
            
            if (annualConsumption >= min && annualConsumption <= max) {
                matchingRange = range;
                break;
            }
        }

        if (matchingRange == null) {
            throw new IllegalArgumentException("No consumption band for " + annualConsumption);
        }

        // Find matching PV generation band
        JsonNode bands = matchingRange.path("bands");
        if (!bands.isArray()) {
            throw new IllegalArgumentException("Invalid format: bands not found");
        }

        for (JsonNode band : bands) {
            double pvMin = band.path("pv_min").asDouble();
            double pvMax = band.path("pv_max").asDouble();
            
            if (pvGenKwh >= pvMin && pvGenKwh <= pvMax) {
                // Find closest battery size
                JsonNode batteries = band.path("batteries");
                if (!batteries.isArray()) {
                    throw new IllegalArgumentException("Invalid format: batteries not found");
                }

                double bestDelta = Double.MAX_VALUE;
                double chosenPercentage = 0;

                for (JsonNode battery : batteries) {
                    double batterySize = Double.parseDouble(battery.path("size").asText());
                    double delta = Math.abs(batteryKwh - batterySize);
                    
                    if (delta < bestDelta) {
                        bestDelta = delta;
                        chosenPercentage = battery.path("pv_generated_percentage").asDouble();
                    }
                }

                return chosenPercentage;
            }
        }

        throw new IllegalArgumentException("No PV band match for " + pvGenKwh);
    }
}
