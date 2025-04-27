package com.example.roi.controller;

import com.example.roi.model.RoiRequest;
import com.example.roi.model.RoiResponse;
import com.example.roi.model.UserProfile;
import com.example.roi.service.RoiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/roi")
public class RoiFormController {

    @Autowired
    private RoiService roiService;

    @GetMapping("/form")
    public String showDashboard(Model model) {
        if (!model.containsAttribute("request")) {
            model.addAttribute("request", new RoiRequest());
        }
        
        // Ensure there's a user profile
        if (!model.containsAttribute("userProfile")) {
            model.addAttribute("userProfile", new UserProfile());
        }
        
        return "results";
    }

    @PostMapping("/calculate")
    public String handleCalculation(@ModelAttribute RoiRequest request, 
                                   @ModelAttribute UserProfile userProfile,
                                   RedirectAttributes redirectAttributes) {
        try {
            RoiResponse response = roiService.calculate(request);
            redirectAttributes.addFlashAttribute("results", response.totalSavings);
            redirectAttributes.addFlashAttribute("request", request);
            redirectAttributes.addFlashAttribute("userProfile", userProfile);
            return "redirect:/roi/form";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error calculating ROI: " + e.getMessage());
            redirectAttributes.addFlashAttribute("request", request);
            redirectAttributes.addFlashAttribute("userProfile", userProfile);
            return "redirect:/roi/form";
        }
    }
}
