
package com.example.roi.service;

import com.example.roi.model.RoiRequest;
import com.example.roi.model.RoiResponse;
import com.example.roi.model.Tariff;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class RoiService {
    public RoiResponse calculate(RoiRequest request) {
        double usableBattery = request.batterySize * 0.90;
        double shiftable = Math.min(usableBattery * 365, request.usage);
        double solarGen = request.solarSize * 850;
        double solarUsed = solarGen * 0.5;
        double solarExport = solarGen * 0.5 * 0.6;

        Map<String, Double> results = new HashMap<>();
        for (Tariff t : request.tariffs) {
            double batterySavings = shiftable * (t.peakRate - t.offpeakRate) * 0.85;
            double solarSavings = solarUsed * t.peakRate + solarExport * t.exportRate;
            double total = batterySavings + solarSavings;
            results.put(t.name, total);
        }
        return new RoiResponse(results);
    }
}
