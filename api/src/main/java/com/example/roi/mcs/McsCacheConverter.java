package com.example.roi.mcs;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;

/**
 * Utility class to convert large MCS CSV files into cached serialized objects
 * for faster loading. This significantly improves application startup time
 * when dealing with large datasets (700K+ rows).
 * 
 * The converter reads the CSV file, parses each row into McsEntry objects,
 * and serializes the entire list to a binary file.
 * 
 * Usage:
 * <pre>
 * McsCacheConverter converter = new McsCacheConverter();
 * converter.convertCsvToCache("mcs_data.csv", "mcs_data.cache");
 * </pre>
 */
public class McsCacheConverter {
    
    /**
     * Converts a CSV file to a cached binary file containing serialized McsEntry objects.
     *
     * @param csvPath Path to the input CSV file
     * @param cachePath Path to the output cache file
     * @throws IOException if file I/O operations fail
     * @throws CsvException if CSV parsing fails
     */
    public void convertCsvToCache(String csvPath, String cachePath) throws IOException, CsvException {
        System.out.println("Starting conversion from CSV to cache...");
        System.out.println("Input CSV: " + csvPath);
        System.out.println("Output cache: " + cachePath);
        
        long startTime = System.currentTimeMillis();
        
        List<McsEntry> entries = loadFromCsv(csvPath);
        
        System.out.println("Loaded " + entries.size() + " entries from CSV");
        
        // Serialize the entries to a binary file
        try (ObjectOutputStream oos = new ObjectOutputStream(
                new BufferedOutputStream(new FileOutputStream(cachePath)))) {
            oos.writeObject(entries);
        }
        
        long endTime = System.currentTimeMillis();
        long durationMs = endTime - startTime;
        
        System.out.println("Conversion completed in " + durationMs + "ms");
        
        // Verify file sizes
        File csvFile = new File(csvPath);
        File cacheFile = new File(cachePath);
        
        System.out.println("CSV file size: " + formatBytes(csvFile.length()));
        System.out.println("Cache file size: " + formatBytes(cacheFile.length()));
        System.out.println("Cache file is " + 
            String.format("%.1f", (double) cacheFile.length() / csvFile.length() * 100) + 
            "% of CSV size");
    }
    
    /**
     * Loads entries from a CSV file and converts them to McsEntry objects.
     *
     * @param csvPath Path to the CSV file
     * @return List of McsEntry objects
     * @throws IOException if file I/O operations fail
     * @throws CsvException if CSV parsing fails
     */
    public List<McsEntry> loadFromCsv(String csvPath) throws IOException, CsvException {
        List<McsEntry> entries = new ArrayList<>();
        int skipCount = 0;
        int processedCount = 0;
        
        try (CSVReader reader = new CSVReader(new FileReader(csvPath))) {
            List<String[]> records = reader.readAll();
            
            System.out.println("Total records in CSV: " + records.size());
            
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
                        
                        entries.add(new McsEntry(
                            occupancyDays, occupancyDaysNormalized,
                            annualConsumptionKwh, pvGenerationKwh,
                            batterySizeKwh, predictedSelfConsumptionPercentage,
                            pvToConsumptionRatio, batteryToConsumptionRatio
                        ));
                        
                        processedCount++;
                        
                        // Progress indicator for large files
                        if (processedCount % 50000 == 0) {
                            System.out.println("Processed " + processedCount + " entries...");
                        }
                        
                    } catch (NumberFormatException e) {
                        skipCount++;
                        if (skipCount <= 10) { // Only log first 10 errors to avoid spam
                            System.err.println("Skipping invalid row " + i + ": " + String.join(",", record));
                            System.err.println("Error: " + e.getMessage());
                        }
                    }
                } else {
                    skipCount++;
                    if (skipCount <= 10) {
                        System.err.println("Skipping incomplete row " + i + " (has " + record.length + " columns, expected 8)");
                    }
                }
            }
        }
        
        if (skipCount > 0) {
            System.out.println("Skipped " + skipCount + " invalid/incomplete rows");
        }
        
        return entries;
    }
    
    /**
     * Loads cached MCS entries from a serialized binary file.
     *
     * @param cachePath Path to the cache file
     * @return List of McsEntry objects
     * @throws IOException if file I/O operations fail
     * @throws ClassNotFoundException if deserialization fails
     */
    @SuppressWarnings("unchecked")
    public List<McsEntry> loadFromCache(String cachePath) throws IOException, ClassNotFoundException {
        System.out.println("Loading MCS data from cache: " + cachePath);
        long startTime = System.currentTimeMillis();
        
        List<McsEntry> entries;
        try (ObjectInputStream ois = new ObjectInputStream(
                new BufferedInputStream(new FileInputStream(cachePath)))) {
            entries = (List<McsEntry>) ois.readObject();
        }
        
        long endTime = System.currentTimeMillis();
        long durationMs = endTime - startTime;
        
        System.out.println("Loaded " + entries.size() + " entries from cache in " + durationMs + "ms");
        
        return entries;
    }
    
    /**
     * Formats byte count in human-readable format.
     *
     * @param bytes Number of bytes
     * @return Formatted string (e.g., "1.5 MB")
     */
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }
    
    /**
     * Main method to run the converter from command line.
     * 
     * Usage: java McsCacheConverter [csv_file] [cache_file]
     * If no arguments provided, uses default paths.
     */
    public static void main(String[] args) {
        String csvPath = "src/main/resources/mcs/mcs_synthetic_dataset.csv";
        String cachePath = "src/main/resources/mcs/mcs_synthetic_dataset.cache";
        
        if (args.length >= 2) {
            csvPath = args[0];
            cachePath = args[1];
        } else if (args.length == 1) {
            System.err.println("Usage: java McsCacheConverter <csv_file> <cache_file>");
            System.err.println("Or run without arguments to use default paths");
            return;
        }
        
        System.out.println("MCS Cache Converter");
        System.out.println("==================");
        
        McsCacheConverter converter = new McsCacheConverter();
        
        try {
            converter.convertCsvToCache(csvPath, cachePath);
            System.out.println("\nConversion successful!");
            System.out.println("You can now use the cache file with McsLookup for faster loading.");
            
            // Test loading the cache to verify it works
            System.out.println("\nTesting cache file...");
            List<McsEntry> testEntries = converter.loadFromCache(cachePath);
            System.out.println("Cache test successful! Loaded " + testEntries.size() + " entries.");
            
        } catch (Exception e) {
            System.err.println("Conversion failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 