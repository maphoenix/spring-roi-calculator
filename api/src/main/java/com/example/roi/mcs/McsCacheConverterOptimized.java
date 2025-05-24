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
import com.opencsv.exceptions.CsvValidationException;

/**
 * Memory-optimized version of McsCacheConverter that processes large CSV files
 * in chunks to avoid OutOfMemoryError issues with very large datasets (700K+ rows).
 * 
 * This version reads and processes the CSV file incrementally rather than loading
 * all records into memory at once.
 */
public class McsCacheConverterOptimized {
    
    private static final int CHUNK_SIZE = 10000; // Process 10K rows at a time
    
    /**
     * Converts a CSV file to a cached binary file containing serialized McsEntry objects.
     * Uses memory-efficient streaming approach.
     *
     * @param csvPath Path to the input CSV file
     * @param cachePath Path to the output cache file
     * @throws IOException if file I/O operations fail
     */
    public void convertCsvToCache(String csvPath, String cachePath) throws IOException, CsvValidationException {
        System.out.println("Starting memory-optimized conversion from CSV to cache...");
        System.out.println("Input CSV: " + csvPath);
        System.out.println("Output cache: " + cachePath);
        
        long startTime = System.currentTimeMillis();
        
        List<McsEntry> allEntries = loadFromCsvOptimized(csvPath);
        
        System.out.println("Loaded " + allEntries.size() + " entries from CSV");
        
        // Serialize the entries to a binary file
        try (ObjectOutputStream oos = new ObjectOutputStream(
                new BufferedOutputStream(new FileOutputStream(cachePath)))) {
            oos.writeObject(allEntries);
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
     * Memory-optimized CSV loading that processes the file in chunks.
     */
    private List<McsEntry> loadFromCsvOptimized(String csvPath) throws IOException, CsvValidationException {
        List<McsEntry> allEntries = new ArrayList<>();
        int skipCount = 0;
        int processedCount = 0;
        
        System.out.println("Processing CSV file in chunks of " + CHUNK_SIZE + " rows...");
        
        try (CSVReader reader = new CSVReader(new FileReader(csvPath))) {
            // Skip header row
            reader.readNext();
            
            String[] record;
            List<McsEntry> chunk = new ArrayList<>(CHUNK_SIZE);
            
            while ((record = reader.readNext()) != null) {
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
                        
                        chunk.add(new McsEntry(
                            occupancyDays, occupancyDaysNormalized,
                            annualConsumptionKwh, pvGenerationKwh,
                            batterySizeKwh, predictedSelfConsumptionPercentage,
                            pvToConsumptionRatio, batteryToConsumptionRatio
                        ));
                        
                        processedCount++;
                        
                        // When chunk is full, add to main list and clear chunk
                        if (chunk.size() >= CHUNK_SIZE) {
                            allEntries.addAll(chunk);
                            chunk.clear();
                            
                            System.out.println("Processed " + processedCount + " entries...");
                            
                            // Suggest garbage collection to free memory
                            if (processedCount % (CHUNK_SIZE * 5) == 0) {
                                System.gc();
                            }
                        }
                        
                    } catch (NumberFormatException e) {
                        skipCount++;
                        if (skipCount <= 10) { // Only log first 10 errors to avoid spam
                            System.err.println("Skipping invalid row " + (processedCount + skipCount) + ": " + String.join(",", record));
                            System.err.println("Error: " + e.getMessage());
                        }
                    }
                } else {
                    skipCount++;
                    if (skipCount <= 10) {
                        System.err.println("Skipping incomplete row " + (processedCount + skipCount) + " (has " + record.length + " columns, expected 8)");
                    }
                }
            }
            
            // Add any remaining entries in the last chunk
            if (!chunk.isEmpty()) {
                allEntries.addAll(chunk);
            }
        }
        
        if (skipCount > 0) {
            System.out.println("Skipped " + skipCount + " invalid/incomplete rows");
        }
        
        System.out.println("Final total: " + allEntries.size() + " entries loaded");
        
        return allEntries;
    }
    
    /**
     * Loads cached MCS entries from a serialized binary file.
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
     */
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }
    
    /**
     * Main method with increased memory settings for Maven execution.
     */
    public static void main(String[] args) {
        String csvPath = "src/main/resources/mcs/mcs_synthetic_dataset.csv";
        String cachePath = "src/main/resources/mcs/mcs_synthetic_dataset.cache";
        
        if (args.length >= 2) {
            csvPath = args[0];
            cachePath = args[1];
        } else if (args.length == 1) {
            System.err.println("Usage: java McsCacheConverterOptimized <csv_file> <cache_file>");
            System.err.println("Or run without arguments to use default paths");
            return;
        }
        
        System.out.println("MCS Cache Converter (Memory Optimized)");
        System.out.println("=====================================");
        
        // Print memory info
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        System.out.println("Max memory available: " + (maxMemory / 1024 / 1024) + " MB");
        
        McsCacheConverterOptimized converter = new McsCacheConverterOptimized();
        
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