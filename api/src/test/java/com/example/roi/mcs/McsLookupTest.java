package com.example.roi.mcs;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class McsLookupTest {

    private McsLookup mcsLookup;
    private static final String TEST_CSV = "/mcs/test_self_consumption.csv";
    
    @BeforeEach
    void setUp(@TempDir Path tempDir) throws Exception {
        // Copy test CSV to temp directory
        Path testFile = tempDir.resolve("test_self_consumption.csv");
        Files.copy(
            getClass().getResourceAsStream(TEST_CSV),
            testFile,
            StandardCopyOption.REPLACE_EXISTING
        );
        mcsLookup = new McsLookup(testFile.toString());
    }

    @Test
    void testLookupWithValidData() {
        // Test case with exact match for Home all day (5 occupancy days)
        double percentage1 = mcsLookup.lookup(
            5,     // occupancy days for "Home all day"
            1750,  // annual consumption 
            400,   // PV generation
            2.1    // battery size
        );
        assertEquals(95.0, percentage1, 0.1);

        // Test case with higher PV generation
        double percentage2 = mcsLookup.lookup(
            5,     // occupancy days for "Home all day"
            1750,  // annual consumption
            700,   // PV generation
            3.1    // battery size
        );
        assertEquals(93.0, percentage2, 0.1);

        // Test case for out during day (1 occupancy days)
        double percentage3 = mcsLookup.lookup(
            1,     // occupancy days for "Out during day"
            1750,  // annual consumption
            400,   // PV generation
            2.1    // battery size
        );
        assertEquals(75.0, percentage3, 0.1);
    }

    @Test
    void testLookupWithInterpolatedBatterySize() {
        // Test with a battery size between available values
        double percentage = mcsLookup.lookup(
            5,     // occupancy days for "Home all day"
            1750,  // annual consumption
            400,   // PV generation
            1.5    // battery size between 1.1 and 2.1
        );
        // Should return closest battery size value (1.1 is closer to 1.5)
        assertEquals(91.2, percentage, 0.1);
    }

    @Test
    void testLookupWithInvalidConsumption() {
        Exception exception = assertThrows(InvalidParameterException.class, () -> {
            mcsLookup.lookup(
                5,
                -100,  // invalid consumption
                400,
                2.1
            );
        });
        assertTrue(exception.getMessage().contains("Annual consumption must be between"));
    }

    @Test
    void testLookupWithInvalidPvGeneration() {
        Exception exception = assertThrows(InvalidParameterException.class, () -> {
            mcsLookup.lookup(
                5,
                1750,
                -100,  // invalid PV generation
                2.1
            );
        });
        assertTrue(exception.getMessage().contains("PV generation must be between"));
    }

    @Test
    void testLookupWithInvalidOccupancy() {
        Exception exception = assertThrows(InvalidParameterException.class, () -> {
            mcsLookup.lookup(
                10,    // invalid occupancy days
                1750,
                400,
                2.1
            );
        });
        assertTrue(exception.getMessage().contains("Occupancy days must be between"));
    }

    @Test
    void testLegacyLookupWithStringOccupancy() {
        // Test backward compatibility with string occupancy types
        double percentage1 = mcsLookup.lookup(1750, 400, 2.1, "Occupancy: Home all day");
        double percentage2 = mcsLookup.lookup(1750, 400, 2.1, "Home all day");
        
        assertEquals(percentage1, percentage2, 0.0001);
        assertEquals(95.0, percentage1, 0.0001);
    }

    @Test
    void testFindClosestMatch() {
        McsLookup.MatchResult result = mcsLookup.findClosestMatch(
            5,                // exact occupancy match for "Home all day"
            1750,             // exact consumption match
            400,              // exact PV generation match
            2.0               // close to 2.1 battery size
        );

        assertEquals(5, result.matchedOccupancyDays);
        assertEquals(1750.0, result.matchedAnnualConsumption, 0.1);
        assertEquals(400.0, result.matchedPvGeneration, 0.1);
        assertEquals(2.1, result.matchedBatterySize, 0.1);
        assertTrue(result.similarity > 0.95); // High similarity expected for close match
    }

    @Test
    void testFindClosestMatchWithApproximateValues() {
        McsLookup.MatchResult result = mcsLookup.findClosestMatch(
            5,                // Home all day
            1800,             // slightly higher consumption
            450,              // slightly higher PV
            3.0               // between 2.1 and 3.1
        );

        assertEquals(5, result.matchedOccupancyDays);
        assertTrue(result.similarity > 0.8); // Good similarity expected
    }

    @Test
    void testFindClosestMatchWithDistantValues() {
        McsLookup.MatchResult result = mcsLookup.findClosestMatch(
            5,                // Home all day
            1750,             // exact consumption
            700,              // higher PV generation
            4.1               // largest battery size
        );

        assertEquals(5, result.matchedOccupancyDays);
        assertEquals(1750.0, result.matchedAnnualConsumption, 0.1);
        assertEquals(700.0, result.matchedPvGeneration, 0.1);
        assertEquals(4.1, result.matchedBatterySize, 0.1);
        assertEquals(94.5, result.percentage, 0.1);
    }

    @ParameterizedTest
    @ValueSource(doubles = {-1.0, -100.0, 25000.0})
    void testInvalidConsumption(double consumption) {
        Exception exception = assertThrows(InvalidParameterException.class, () -> {
            mcsLookup.findClosestMatch(
                5,
                consumption,
                400,
                2.1
            );
        });
        assertTrue(exception.getMessage().contains("Annual consumption must be between"));
    }

    @ParameterizedTest
    @ValueSource(doubles = {-1.0, -50.0, 15000.0})
    void testInvalidPvGeneration(double pvGen) {
        Exception exception = assertThrows(InvalidParameterException.class, () -> {
            mcsLookup.findClosestMatch(
                5,
                1750,
                pvGen,
                2.1
            );
        });
        assertTrue(exception.getMessage().contains("PV generation must be between"));
    }

    @ParameterizedTest
    @ValueSource(doubles = {-1.0, -5.0, 60.0})
    void testInvalidBatterySize(double batterySize) {
        Exception exception = assertThrows(InvalidParameterException.class, () -> {
            mcsLookup.findClosestMatch(
                5,
                1750,
                400,
                batterySize
            );
        });
        assertTrue(exception.getMessage().contains("Battery size must be between"));
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 6, 10})
    void testInvalidOccupancyDays(int occupancyDays) {
        Exception exception = assertThrows(InvalidParameterException.class, () -> {
            mcsLookup.findClosestMatch(
                occupancyDays,
                1750,
                400,
                2.1
            );
        });
        assertTrue(exception.getMessage().contains("Occupancy days must be between"));
    }

    @Test
    void testBoundaryValues() {
        // Test minimum boundary values
        assertDoesNotThrow(() -> {
            mcsLookup.findClosestMatch(1, 0.0, 0.0, 0.0);
        });

        // Test maximum boundary values
        assertDoesNotThrow(() -> {
            mcsLookup.findClosestMatch(5, 20000.0, 10000.0, 50.0);
        });
    }

    @Test
    void testGetEntryCount() {
        // Test that entries are loaded correctly
        int entryCount = mcsLookup.getEntryCount();
        assertTrue(entryCount > 0, "CSV should contain entries");
        assertEquals(25, entryCount, "Expected 25 entries from test CSV");
    }

    @Test
    void testOccupancyStringConversion() {
        // Test various occupancy string formats
        double percentage1 = mcsLookup.lookup(1750, 400, 2.1, "Home all day");
        double percentage2 = mcsLookup.lookup(1750, 400, 2.1, "out during day");  
        double percentage3 = mcsLookup.lookup(1750, 400, 2.1, "in half day");
        double percentage4 = mcsLookup.lookup(1750, 400, 2.1, "hybrid");

        // Verify they return expected values based on occupancy conversion
        assertEquals(95.0, percentage1, 0.1); // 5 days -> Home all day
        assertEquals(75.0, percentage2, 0.1); // 1 day -> Out during day
        assertEquals(85.0, percentage3, 0.1); // 3 days -> In half day
        assertEquals(85.0, percentage4, 0.1); // 2 days -> Hybrid
    }
} 