package com.example.roi.pdf;

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

import com.example.roi.model.RoiCalculationResponse;
import com.example.roi.model.RoiRequest;
import com.example.roi.service.RoiService;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
class RoiPdfReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RoiService roiService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void report_endpoint_acceptsRoiCalculationResponse() throws Exception {
        // Build a sample RoiRequest
        RoiRequest request = new RoiRequest();
        request.setBatterySize(5.0);
        request.setSolarSize(4.0);
        request.setUsage(4000);
        request.setIncludePdfBreakdown(true);

        // Use the service to get a valid RoiCalculationResponse
        RoiCalculationResponse response = roiService.calculate(request);
        String responseJson = objectMapper.writeValueAsString(response);

        // POST the valid RoiCalculationResponse to /report
        mockMvc.perform(post("/api/roi/report")
                .contentType(MediaType.APPLICATION_JSON)
                .content(responseJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pdfUrl").exists())
                .andExpect(jsonPath("$.pdfUrl", containsString("/api/roi/reports/roi-report-")));
    }
} 