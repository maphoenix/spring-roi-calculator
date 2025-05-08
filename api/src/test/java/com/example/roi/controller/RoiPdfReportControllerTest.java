package com.example.roi.controller;

import static org.hamcrest.Matchers.containsString;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RoiPdfReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void calculate_returnsFlatResponseWithPdfUrl() throws Exception {
        String requestJson = """
            {
                \"batterySize\": 5.0,
                \"solarSize\": 4.0,
                \"usage\": 4000,
                \"includePdfBreakdown\": true
            }
            """;

        mockMvc.perform(post("/api/roi/calculate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pdfUrl").exists())
                .andExpect(jsonPath("$.pdfUrl", containsString("/api/roi/reports/roi-report-")))
                .andExpect(jsonPath("$.totalCost.amount").exists());
    }
} 