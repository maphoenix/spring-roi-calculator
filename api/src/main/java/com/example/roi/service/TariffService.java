package com.example.roi.service;

import com.example.roi.model.Tariff;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class TariffService {
    
    /**
     * Returns the list of tariffs, cached for performance
     * 
     * @return List of available tariffs
     */
    @Cacheable("tariffs")
    public List<Tariff> getAvailableTariffs() {
        // In a real application, this would fetch from an API or database
        List<Tariff> tariffs = new ArrayList<>();
        
        // Add default tariffs
        tariffs.add(new Tariff("Intelligent Octopus Go", 0.2771, 0.075, 0.15));
        tariffs.add(new Tariff("Octopus Flux", 0.2758, 0.1655, 0.2922));
        tariffs.add(new Tariff("EDF GoElectric", 0.2980, 0.0899, 0.1850));
        tariffs.add(new Tariff("OVO Energy", 0.2790, 0.1299, 0.1650));
        tariffs.add(new Tariff("Bulb Smart Tariff", 0.2810, 0.1180, 0.1720));
        
        return tariffs;
    }
    
    /**
     * Clears the tariff cache at midnight every day
     */
    @CacheEvict(value = "tariffs", allEntries = true)
    @Scheduled(cron = "0 0 0 * * ?") // Run at midnight every day
    public void clearTariffCache() {
        // Method intentionally empty - just clearing the cache
        System.out.println("Tariff cache cleared at midnight");
    }
} 