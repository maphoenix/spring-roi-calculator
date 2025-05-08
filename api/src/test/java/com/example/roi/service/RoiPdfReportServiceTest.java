package com.example.roi.service;

import java.io.FileOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.example.roi.model.RoiCalculationResponse;
import com.example.roi.model.RoiRequest;
import com.lowagie.text.DocumentException;

@SpringBootTest
class RoiPdfReportServiceTest {

    @Autowired
    private RoiPdfReportService pdfReportService;

    @Autowired
    private RoiService roiService;

    @Test
    void generateRoiReport_withExampleResponse_producesNonEmptyPdf() throws IOException, DocumentException {
        // Create a sample RoiRequest with includePdfBreakdown set to true
        RoiRequest request = new RoiRequest();
        request.setBatterySize(5.0);
        request.setSolarSize(4.0);
        request.setUsage(4000);
        request.setIncludePdfBreakdown(true);

        // Generate the calculation response using the service
        RoiCalculationResponse response = roiService.calculate(request);

        // Generate the PDF
        byte[] pdfBytes = pdfReportService.generateRoiReport(response);

        // Assert the PDF is not empty
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 100, "PDF should not be empty");

        // Optionally, write to a file for manual inspection
        try (FileOutputStream fos = new FileOutputStream("test-roi-report.pdf")) {
            fos.write(pdfBytes);
        }
    }
} 