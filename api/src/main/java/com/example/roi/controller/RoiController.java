package com.example.roi.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class RoiController {
    
    @GetMapping("/documentation")
    public String showDocumentation(Model model) {
        return "documentation";
    }
}