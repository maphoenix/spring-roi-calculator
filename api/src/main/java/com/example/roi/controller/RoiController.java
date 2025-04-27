package com.example.roi.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class RoiController {
    
    @GetMapping("/")
    public String redirectToForm() {
        return "redirect:/roi/form";
    }
    
    @GetMapping("/documentation")
    public String showDocumentation() {
        return "redirect:/roi-documentation.html";
    }
}