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
    private static final String TEST_JSON = "/mcs/test_self_consumption.json";
    
    @BeforeEach
    void setUp(@TempDir Path tempDir) throws Exception {
        // Copy test JSON to temp directory
        Path testFile = tempDir.resolve("test_self_consumption.json");
        Files.copy(
            getClass().getResourceAsStream(TEST_JSON),
            testFile,
            StandardCopyOption.REPLACE_EXISTING
        );
        mcsLookup = new McsLookup(testFile.toString());
    }

    @Test
    void testLookupWithValidData() {
        // Test case within 300-599 PV range
        double fraction1 = mcsLookup.lookup(
            1750,  // annual consumption within 1500-1999 range
            400,   // PV generation within 300-599 range
            2.1,   // battery size
            "Occupancy: Home all day"
        );
        assertEquals(0.95, fraction1, 0.0001);

        // Test case within 600-899 PV range
        double fraction2 = mcsLookup.lookup(
            1750,  // annual consumption within 1500-1999 range
            700,   // PV generation within 600-899 range
            3.1,   // battery size
            "Occupancy: Home all day"
        );
        assertEquals(0.93, fraction2, 0.0001);
    }

    @Test
    void testLookupWithInterpolatedBatterySize() {
        // Test with a battery size between available values
        double fraction = mcsLookup.lookup(
            1750,  // annual consumption within 1500-1999 range
            400,   // PV generation within 300-599 range
            1.5,   // battery size between 1.1 and 2.1
            "Occupancy: Home all day"
        );
        // Should return closest battery size value (1.1 in this case)
        assertEquals(0.9123704114490263, fraction, 0.0001);
    }

    @Test
    void testLookupWithInvalidConsumption() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            mcsLookup.lookup(
                1000,  // consumption outside valid range
                400,
                2.1,
                "Occupancy: Home all day"
            );
        });
        assertTrue(exception.getMessage().contains("No consumption band for 1000"));
    }

    @Test
    void testLookupWithInvalidPvGeneration() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            mcsLookup.lookup(
                1750,
                1000,  // PV generation outside valid range
                2.1,
                "Occupancy: Home all day"
            );
        });
        assertTrue(exception.getMessage().contains("No PV band match for 1000"));
    }

    @Test
    void testLookupWithInvalidOccupancy() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            mcsLookup.lookup(
                1750,
                400,
                2.1,
                "Invalid Occupancy"
            );
        });
        assertTrue(exception.getMessage().contains("Occupancy not found"));
    }

    @Test
    void testNormalizedOccupancyKey() {
        // Both formats should work
        double fraction1 = mcsLookup.lookup(1750, 400, 2.1, "Occupancy: Home all day");
        double fraction2 = mcsLookup.lookup(1750, 400, 2.1, "Home all day");
        
        assertEquals(fraction1, fraction2, 0.0001);
        assertEquals(0.95, fraction1, 0.0001);
    }

    @Test
    void testFindClosestMatch() {
        McsLookup.MatchResult result = mcsLookup.findClosestMatch(
            "Home all day",    // exact occupancy match
            1600,             // in range 1500-1999
            350,              // close to 300-599 range
            2.0               // close to 2.1 battery size
        );

        assertEquals("Home all day", result.matchedOccupancy);
        assertTrue(result.matchedConsumption.contains("1,500 kWh to 1,999 kWh"));
        assertEquals("300-599", result.matchedPvRange);
        assertEquals(2.1, result.matchedBatterySize, 0.0001);
        assertTrue(result.similarity > 0.8); // High similarity expected
    }

    @Test
    void testFindClosestMatchWithApproximateValues() {
        McsLookup.MatchResult result = mcsLookup.findClosestMatch(
            "Home All Day",    // slightly different formatting
            2000,             // just outside range
            590,              // at edge of range
            3.0               // between 2.1 and 3.1
        );

        assertEquals("Home all day", result.matchedOccupancy);
        assertTrue(result.matchedConsumption.contains("1,500 kWh to 1,999 kWh"));
        assertEquals("300-599", result.matchedPvRange);
        assertTrue(result.similarity > 0.7); // Good similarity expected
    }

    @Test
    void testFindClosestMatchWithDistantValues() {
        McsLookup.MatchResult result = mcsLookup.findClosestMatch(
            "Home all day",
            1750,             // in range
            700,              // in 600-899 range
            4.1               // largest battery size
        );

        assertEquals("Home all day", result.matchedOccupancy);
        assertTrue(result.matchedConsumption.contains("1,500 kWh to 1,999 kWh"));
        assertEquals("600-899", result.matchedPvRange);
        assertEquals(4.1, result.matchedBatterySize, 0.0001);
        assertEquals(0.9448009138806771, result.fraction, 0.0001);
    }

    @Test
    void testNullOccupancyType() {
        Exception exception = assertThrows(InvalidParameterException.class, () -> {
            mcsLookup.findClosestMatch(
                null,
                1750,
                400,
                2.1
            );
        });
        assertTrue(exception.getMessage().contains("cannot be null or empty"));
    }

    @Test
    void testEmptyOccupancyType() {
        Exception exception = assertThrows(InvalidParameterException.class, () -> {
            mcsLookup.findClosestMatch(
                "   ",  // whitespace only
                1750,
                400,
                2.1
            );
        });
        assertTrue(exception.getMessage().contains("cannot be null or empty"));
    }

    @ParameterizedTest
    @ValueSource(doubles = {-1.0, -100.0, 25000.0})
    void testInvalidAnnualConsumption(double consumption) {
        Exception exception = assertThrows(InvalidParameterException.class, () -> {
            mcsLookup.findClosestMatch(
                "Home all day",
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
                "Home all day",
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
                "Home all day",
                1750,
                400,
                batterySize
            );
        });
        assertTrue(exception.getMessage().contains("Battery size must be between"));
    }

    @Test
    void testBoundaryValues() {
        // Test minimum valid values
        assertDoesNotThrow(() -> {
            mcsLookup.findClosestMatch(
                "Home all day",
                0.0,    // MIN_CONSUMPTION
                0.0,    // MIN_PV_GENERATION
                0.0     // MIN_BATTERY_SIZE
            );
        });

        // Test maximum valid values
        assertDoesNotThrow(() -> {
            mcsLookup.findClosestMatch(
                "Home all day",
                20000.0,  // MAX_CONSUMPTION
                10000.0,  // MAX_PV_GENERATION
                50.0      // MAX_BATTERY_SIZE
            );
        });
    }
} 