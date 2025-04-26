
package com.example.roi.controller;

import com.example.roi.model.RoiRequest;
import com.example.roi.model.RoiResponse;
import com.example.roi.service.RoiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/roi")
public class RoiFormController {

    @Autowired
    private RoiService roiService;

    @GetMapping("/form")
    public String showForm(Model model) {
        model.addAttribute("request", new RoiRequest());
        return "form";
    }

    @PostMapping("/form")
    public String handleFormSubmit(RoiRequest request, Model model) {
        RoiResponse response = roiService.calculate(request);
        model.addAttribute("results", response.totalSavings);
        return "form";
    }
}
