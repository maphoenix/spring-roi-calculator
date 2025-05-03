package com.example.roi.service;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.roi.model.RoiRequest;
import com.example.roi.model.RoiRequest.CardinalDirection;
import com.example.roi.model.RoiResponse;
import com.example.roi.model.Tariff;
import com.example.roi.model.UserProfile;

public class RoiServiceTest {

    private RoiService roiService;
    private TariffService tariffService;

    @BeforeEach
    void setUp() {
        tariffService = mock(TariffService.class);
        roiService = new RoiService();
        // Use reflection to inject the mock TariffService
        try {
            var field = RoiService.class.getDeclaredField("tariffService");
            field.setAccessible(true);
            field.set(roiService, tariffService);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testCalculate_withAllNewFieldsAndEnumDirection() {
        // Arrange
        Tariff tariff = new Tariff("Test Tariff", 0.30, 0.10, 0.15);
        when(tariffService.getAvailableTariffs()).thenReturn(Arrays.asList(tariff));

        RoiRequest request = new RoiRequest();
        request.setSolarPanelDirection(CardinalDirection.NORTH);
        request.setHaveOrWillGetEv(true);
        request.setHomeOccupancyDuringWorkHours(true);
        request.setNeedFinance(false);
        request.setBatterySize(15.0);
        request.setUsage(4500);
        request.setSolarSize(5.0);

        // Act
        RoiResponse response = roiService.calculate(request);

        // Assert
        assertNotNull(response);
        assertNotNull(response.totalSavings);
        assertTrue(response.totalSavings.containsKey("Test Tariff"));
        assertNotNull(response.timeSeriesData);
        assertTrue(response.timeSeriesData.getYearlyData().containsKey("Test Tariff"));
        assertTrue(response.timeSeriesData.getCumulativeData().containsKey("Test Tariff"));
        assertTrue(response.timeSeriesData.getPaybackPeriod().containsKey("Test Tariff"));
    }

    @Test
    void testDeriveRequestDefaults_forLargeHouseWithEv() {
        UserProfile profile = new UserProfile();
        profile.setHouseSize(UserProfile.HouseSize.LARGE);
        profile.setHasOrPlanningEv(true);
        profile.setHomeOccupiedDuringDay(false);

        RoiRequest request = roiService.deriveRequestDefaults(profile);

        assertEquals(25.0, request.getBatterySize());
        assertEquals(8500, request.getUsage()); // 6000 + 2500 for EV
        assertEquals(6.0, request.getSolarSize());
    }
} 