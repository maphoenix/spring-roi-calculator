package com.example.roi.controller;

import com.example.roi.model.RoiRequest;
import com.example.roi.model.RoiResponse;
import com.example.roi.service.RoiService;
import com.example.roi.service.TariffService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/roi")
public class RoiFormController {

    @Autowired
    private RoiService roiService;
    
    @Autowired
    private TariffService tariffService;

    @GetMapping("/form")
    public String showForm(Model model) {
        if (!model.containsAttribute("request")) {
            RoiRequest request = new RoiRequest();
            // Use only the first two tariffs for the form by default
            if (request.getTariffs().isEmpty() && !tariffService.getAvailableTariffs().isEmpty()) {
                request.setTariffs(tariffService.getAvailableTariffs().subList(0, 2));
            }
            model.addAttribute("request", request);
        }
        
        // Add all available tariffs to the model for the dropdown
        model.addAttribute("availableTariffs", tariffService.getAvailableTariffs());
        
        return "form";
    }

    @PostMapping("/form")
    public String handleFormSubmit(@ModelAttribute RoiRequest request, Model model) {
        try {
            RoiResponse response = roiService.calculate(request);
            model.addAttribute("results", response.totalSavings);
            model.addAttribute("request", request);
            // Add all available tariffs to the model for the dropdown
            model.addAttribute("availableTariffs", tariffService.getAvailableTariffs());
            return "form";
        } catch (Exception e) {
            model.addAttribute("error", "Error calculating ROI: " + e.getMessage());
            model.addAttribute("request", request);
            // Add all available tariffs to the model for the dropdown
            model.addAttribute("availableTariffs", tariffService.getAvailableTariffs());
            return "form";
        }
    }
}
