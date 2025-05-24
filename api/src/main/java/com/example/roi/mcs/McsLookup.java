package com.example.roi.mcs;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;

/**
 * McsLookup provides functionality to find self-consumption percentages from MCS lookup tables.
 * It supports both exact matches and finding the closest matching data point based on multiple criteria.
 * 
 * The class handles CSV data with the following columns:
 * - occupancy_days: Number of days at home (1-5)
 * - occupancy_days_normalized: Normalized occupancy (0.2-1.0)
 * - annual_consumption_kwh: Annual consumption in kWh
 * - pv_generation_kwh: Annual PV generation in kWh
 * - battery_size_kwh: Battery size in kWh
 * - predicted_self_consumption_percentage: Self-consumption percentage (0-100)
 * - pv_to_consumption_ratio: PV to consumption ratio
 * - battery_to_consumption_ratio: Battery to consumption ratio
 *
 * The matching algorithm prioritizes criteria in the following order:
 * 1. Occupancy days (40% weight)
 * 2. Annual consumption (30% weight)
 * 3. PV generation (20% weight)
 * 4. Battery size (10% weight)
 *
 * Input Validation Rules:
 * - Occupancy days: Must be between 1 and 5
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
 * McsLookup lookup = new McsLookup("path/to/mcs_data.csv");
 * 
 * // Find exact match
 * double percentage = lookup.lookup(
 *     5,                // occupancy days
 *     1750,             // annual consumption (kWh)
 *     400,              // PV generation (kWh)
 *     2.1               // battery size (kWh)
 * );
 * 
 * // Find closest match with similarity score
 * MatchResult result = lookup.findClosestMatch(
 *     5,                // occupancy days
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
     * Represents a single entry in the lookup table with all parameters and percentage value.
     */
    public static class Entry {
        public final int occupancyDays;
        public final double occupancyDaysNormalized;
        public final double annualConsumptionKwh;
        public final double pvGenerationKwh;
        public final double batterySizeKwh;
        public final double predictedSelfConsumptionPercentage;
        public final double pvToConsumptionRatio;
        public final double batteryToConsumptionRatio;
        
        public Entry(int occupancyDays, double occupancyDaysNormalized, 
                    double annualConsumptionKwh, double pvGenerationKwh,
                    double batterySizeKwh, double predictedSelfConsumptionPercentage,
                    double pvToConsumptionRatio, double batteryToConsumptionRatio) {
            this.occupancyDays = occupancyDays;
            this.occupancyDaysNormalized = occupancyDaysNormalized;
            this.annualConsumptionKwh = annualConsumptionKwh;
            this.pvGenerationKwh = pvGenerationKwh;
            this.batterySizeKwh = batterySizeKwh;
            this.predictedSelfConsumptionPercentage = predictedSelfConsumptionPercentage;
            this.pvToConsumptionRatio = pvToConsumptionRatio;
            this.batteryToConsumptionRatio = batteryToConsumptionRatio;
        }
    }

    /**
     * Contains the results of a closest match search, including all matched parameters
     * and a similarity score indicating the quality of the match.
     */
    public static class MatchResult {
        /** The matched occupancy days */
        public final int matchedOccupancyDays;
        /** The matched annual consumption in kWh */
        public final double matchedAnnualConsumption;
        /** The matched PV generation in kWh */
        public final double matchedPvGeneration;
        /** The matched battery size in kWh */
        public final double matchedBatterySize;
        /** The self-consumption percentage for this combination */
        public final double percentage;
        /** Similarity score (0-1) indicating how close the match is to the requested values */
        public final double similarity;

        public MatchResult(int matchedOccupancyDays, double matchedAnnualConsumption, 
                         double matchedPvGeneration, double matchedBatterySize, 
                         double percentage, double similarity) {
            this.matchedOccupancyDays = matchedOccupancyDays;
            this.matchedAnnualConsumption = matchedAnnualConsumption;
            this.matchedPvGeneration = matchedPvGeneration;
            this.matchedBatterySize = matchedBatterySize;
            this.percentage = percentage;
            this.similarity = similarity;
        }
    }

    /** The list of all entries loaded from the CSV file */
    private final List<Entry> entries;
    
    /** Minimum allowed values for parameters */
    private static final int MIN_OCCUPANCY_DAYS = 1;
    private static final double MIN_CONSUMPTION = 0.0;
    private static final double MIN_PV_GENERATION = 0.0;
    private static final double MIN_BATTERY_SIZE = 0.0;
    
    /** Maximum allowed values for parameters (can be adjusted based on requirements) */
    private static final int MAX_OCCUPANCY_DAYS = 5;
    private static final double MAX_CONSUMPTION = 20000.0;  // 20,000 kWh
    private static final double MAX_PV_GENERATION = 10000.0; // 10,000 kWh
    private static final double MAX_BATTERY_SIZE = 50.0;    // 50 kWh

    /**
     * Creates a new McsLookup instance by loading data from a CSV file.
     *
     * @param csvPath Path to the CSV file containing the lookup table data
     * @throws IOException if the file cannot be read
     * @throws CsvException if the CSV cannot be parsed
     */
    public McsLookup(String csvPath) throws IOException, CsvException {
        this.entries = loadFromCsv(csvPath);
    }

    /**
     * Loads data from CSV file into a list of Entry objects.
     *
     * @param csvPath Path to the CSV file
     * @return List of Entry objects
     * @throws IOException if the file cannot be read
     * @throws CsvException if the CSV cannot be parsed
     */
    private List<Entry> loadFromCsv(String csvPath) throws IOException, CsvException {
        List<Entry> entries = new ArrayList<>();
        
        try (CSVReader reader = new CSVReader(new FileReader(csvPath))) {
            List<String[]> records = reader.readAll();
            
            // Skip header row (first row)
            for (int i = 1; i < records.size(); i++) {
                String[] record = records.get(i);
                
                if (record.length >= 8) {
                    try {
                        int occupancyDays = Integer.parseInt(record[0].trim());
                        double occupancyDaysNormalized = Double.parseDouble(record[1].trim());
                        double annualConsumptionKwh = Double.parseDouble(record[2].trim());
                        double pvGenerationKwh = Double.parseDouble(record[3].trim());
                        double batterySizeKwh = Double.parseDouble(record[4].trim());
                        double predictedSelfConsumptionPercentage = Double.parseDouble(record[5].trim());
                        double pvToConsumptionRatio = Double.parseDouble(record[6].trim());
                        double batteryToConsumptionRatio = Double.parseDouble(record[7].trim());
                        
                        entries.add(new Entry(
                            occupancyDays, occupancyDaysNormalized,
                            annualConsumptionKwh, pvGenerationKwh,
                            batterySizeKwh, predictedSelfConsumptionPercentage,
                            pvToConsumptionRatio, batteryToConsumptionRatio
                        ));
                    } catch (NumberFormatException e) {
                        // Skip invalid rows
                        System.err.println("Skipping invalid row " + i + ": " + String.join(",", record));
                    }
                }
            }
        }
        
        return entries;
    }

    /**
     * Validates input parameters against allowed ranges.
     * 
     * @throws InvalidParameterException if any parameter is outside its allowed range
     */
    private void validateInputParameters(int occupancyDays,
                                       double annualConsumption,
                                       double pvGenKwh,
                                       double batteryKwh) {
        if (occupancyDays < MIN_OCCUPANCY_DAYS || occupancyDays > MAX_OCCUPANCY_DAYS) {
            throw new InvalidParameterException(
                String.format("Occupancy days must be between %d and %d, got: %d",
                    MIN_OCCUPANCY_DAYS, MAX_OCCUPANCY_DAYS, occupancyDays)
            );
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
     * Calculates similarity between two numeric values.
     * Returns 1.0 for exact matches and decreases based on relative difference.
     *
     * @param value1 First value
     * @param value2 Second value
     * @param maxDifference Maximum expected difference for normalization
     * @return Similarity score between 0 and 1
     */
    private double calculateNumericSimilarity(double value1, double value2, double maxDifference) {
        double difference = Math.abs(value1 - value2);
        return Math.max(0.0, 1.0 - (difference / maxDifference));
    }

    /**
     * Finds the closest matching data point based on multiple criteria.
     * The matching algorithm uses weighted similarity scores for each criterion:
     * - Occupancy days: 40% (exact match preferred)
     * - Annual consumption: 30% (using relative difference)
     * - PV generation: 20% (using relative difference)
     * - Battery size: 10% (using relative difference)
     *
     * Input parameters are validated against the following rules:
     * - occupancyDays: Must be between 1 and 5
     * - annualConsumption: Must be between 0 and 20,000 kWh
     * - pvGenKwh: Must be between 0 and 10,000 kWh
     * - batteryKwh: Must be between 0 and 50 kWh
     *
     * @param occupancyDays Number of days at home (1-5)
     * @param annualConsumption Annual consumption in kWh (0-20,000)
     * @param pvGenKwh Annual PV generation in kWh (0-10,000)
     * @param batteryKwh Battery size in kWh (0-50)
     * @return MatchResult containing the best matching data point and similarity score
     * @throws InvalidParameterException if any input parameter is outside its allowed range
     * @throws IllegalArgumentException if no valid matches can be found in the data
     */
    public MatchResult findClosestMatch(int occupancyDays,
                                      double annualConsumption,
                                      double pvGenKwh,
                                      double batteryKwh) {
        // Validate input parameters
        validateInputParameters(occupancyDays, annualConsumption, pvGenKwh, batteryKwh);

        if (entries.isEmpty()) {
            throw new IllegalArgumentException("No data loaded from CSV file");
        }

        double bestSimilarity = -1;
        Entry bestMatch = null;

        for (Entry entry : entries) {
            // Calculate similarity for each parameter
            double occupancySimilarity = (entry.occupancyDays == occupancyDays) ? 1.0 : 0.0;
            
            double consumptionSimilarity = calculateNumericSimilarity(
                entry.annualConsumptionKwh, annualConsumption, MAX_CONSUMPTION
            );
            
            double pvSimilarity = calculateNumericSimilarity(
                entry.pvGenerationKwh, pvGenKwh, MAX_PV_GENERATION
            );
            
            double batterySimilarity = calculateNumericSimilarity(
                entry.batterySizeKwh, batteryKwh, MAX_BATTERY_SIZE
            );
            
            // Calculate overall similarity using weighted components
            double totalSimilarity = (
                occupancySimilarity * 0.4 +      // 40% weight for occupancy
                consumptionSimilarity * 0.3 +    // 30% weight for consumption
                pvSimilarity * 0.2 +             // 20% weight for PV generation
                batterySimilarity * 0.1          // 10% weight for battery size
            );
            
            if (totalSimilarity > bestSimilarity) {
                bestSimilarity = totalSimilarity;
                bestMatch = entry;
            }
        }

        if (bestMatch == null) {
            throw new IllegalArgumentException("No valid matches found in the data");
        }

        return new MatchResult(
            bestMatch.occupancyDays,
            bestMatch.annualConsumptionKwh,
            bestMatch.pvGenerationKwh,
            bestMatch.batterySizeKwh,
            bestMatch.predictedSelfConsumptionPercentage,
            bestSimilarity
        );
    }

    /**
     * Lookup self-consumption percentage using the new CSV-based approach.
     * This method finds the closest match and returns the percentage.
     *
     * @param occupancyDays      Number of days at home (1-5)
     * @param annualConsumption  Annual consumption in kWh
     * @param pvGenKwh           Annual PV generation in kWh
     * @param batteryKwh         Battery size in kWh
     * @return The self-consumption percentage (0-100)
     */
    public double lookup(int occupancyDays,
                        double annualConsumption,
                        double pvGenKwh,
                        double batteryKwh) {
        MatchResult result = findClosestMatch(occupancyDays, annualConsumption, pvGenKwh, batteryKwh);
        return result.percentage;
    }

    /**
     * Legacy method for backward compatibility.
     * Converts occupancy string to occupancy days and calls the new lookup method.
     *
     * @param annualConsumption  Annual consumption in kWh
     * @param pvGenKwh           Annual PV generation in kWh
     * @param batteryKwh         Battery size in kWh
     * @param occupancyKey       Occupancy type string (converted to days)
     * @return The self-consumption percentage (0-100)
     */
    public double lookup(double annualConsumption,
                        double pvGenKwh,
                        double batteryKwh,
                        String occupancyKey) {
        int occupancyDays = convertOccupancyStringToDays(occupancyKey);
        return lookup(occupancyDays, annualConsumption, pvGenKwh, batteryKwh);
    }

    /**
     * Converts occupancy string to occupancy days for backward compatibility.
     *
     * @param occupancyKey The occupancy key string
     * @return Number of occupancy days (1-5)
     */
    private int convertOccupancyStringToDays(String occupancyKey) {
        String normalized = occupancyKey.toLowerCase().replace("occupancy: ", "").trim();
        
        switch (normalized) {
            case "home all day":
                return 5;  // Home all day = 5 days
            case "out during day":
                return 1;  // Out during day = 1 day
            case "in half day":
                return 3;  // In half day = 3 days
            case "hybrid":
                return 2;  // Hybrid = 2 days
            default:
                // Try to extract number if it's a direct number
                try {
                    int days = Integer.parseInt(normalized);
                    if (days >= MIN_OCCUPANCY_DAYS && days <= MAX_OCCUPANCY_DAYS) {
                        return days;
                    }
                } catch (NumberFormatException e) {
                    // Fall through to exception
                }
                throw new IllegalArgumentException("Unknown occupancy type: " + occupancyKey);
        }
    }

    /**
     * Gets the total number of entries loaded from the CSV file.
     *
     * @return Number of entries
     */
    public int getEntryCount() {
        return entries.size();
    }
}
