package com.example.roi.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.roi.model.RoiRequest;
import com.example.roi.model.RoiResponse;
import com.example.roi.service.RoiService;

@Controller
@RequestMapping("/roi")
public class RoiFormController {

    @Autowired
    private RoiService roiService;

    @GetMapping("/form")
    public String showForm(Model model) {
        if (!model.containsAttribute("request")) {
            model.addAttribute("request", new RoiRequest());
        }
        
        return "form";
    }

    @PostMapping("/form")
    public String handleFormSubmit(@ModelAttribute RoiRequest request, Model model) {
        try {
            RoiResponse response = roiService.calculate(request);
            model.addAttribute("results", response.totalSavings);
            model.addAttribute("request", request);
            return "form";
        } catch (Exception e) {
            model.addAttribute("error", "Error calculating ROI: " + e.getMessage());
            model.addAttribute("request", request);
            return "form";
        }
    }
}
