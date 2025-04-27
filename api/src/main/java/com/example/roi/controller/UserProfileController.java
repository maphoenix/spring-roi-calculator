package com.example.roi.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.roi.model.RoiRequest;
import com.example.roi.model.UserProfile;
import com.example.roi.model.UserProfile.HouseSize;
import com.example.roi.model.UserProfile.TaxBracket;
import com.example.roi.service.RoiService;

@Controller
public class UserProfileController {
    
    @Autowired
    private RoiService roiService;
    
    @GetMapping("/")
    public String showProfileForm(Model model) {
        if (!model.containsAttribute("userProfile")) {
            model.addAttribute("userProfile", new UserProfile());
        }
        return "user-profile";
    }
    
    @PostMapping("/profile-submit")
    public String handleProfileSubmit(@ModelAttribute UserProfile userProfile, 
                                     RedirectAttributes redirectAttributes) {
        try {
            // Use the profile to derive ROI request defaults
            RoiRequest derivedRequest = roiService.deriveRequestDefaults(userProfile);
            
            // Add the derived request and original profile to redirect attributes
            redirectAttributes.addFlashAttribute("request", derivedRequest);
            redirectAttributes.addFlashAttribute("userProfile", userProfile);
            
            return "redirect:/roi/form";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error processing your profile: " + e.getMessage());
            redirectAttributes.addFlashAttribute("userProfile", userProfile);
            return "redirect:/";
        }
    }
    
    @PostMapping("/profile-update")
    public String handleProfileUpdate(@RequestParam("field") String field,
                                     @RequestParam("value") String value,
                                     @ModelAttribute("userProfile") UserProfile userProfile,
                                     RedirectAttributes redirectAttributes) {
        try {
            // Update the specified field in the user profile
            switch (field) {
                case "houseSize":
                    userProfile.setHouseSize(HouseSize.valueOf(value));
                    break;
                case "hasOrPlanningEv":
                    userProfile.setHasOrPlanningEv(Boolean.parseBoolean(value));
                    break;
                case "taxBracket":
                    userProfile.setTaxBracket(TaxBracket.valueOf(value));
                    break;
                case "homeOccupiedDuringDay":
                    userProfile.setHomeOccupiedDuringDay(Boolean.parseBoolean(value));
                    break;
                case "needsFinancing":
                    userProfile.setNeedsFinancing(Boolean.parseBoolean(value));
                    break;
                default:
                    throw new IllegalArgumentException("Unknown field: " + field);
            }
            
            // Use the updated profile to derive new ROI request defaults
            RoiRequest derivedRequest = roiService.deriveRequestDefaults(userProfile);
            
            // Add the derived request and updated profile to redirect attributes
            redirectAttributes.addFlashAttribute("request", derivedRequest);
            redirectAttributes.addFlashAttribute("userProfile", userProfile);
            
            return "redirect:/roi/form";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error updating profile: " + e.getMessage());
            redirectAttributes.addFlashAttribute("userProfile", userProfile);
            return "redirect:/roi/form";
        }
    }
} 