package com.example.roi.controller;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.roi.model.PdfReportLinkResponse;
import com.example.roi.model.RoiCalculationResponse;
import com.example.roi.model.RoiRequest;
import com.example.roi.service.RoiPdfReportService;
import com.example.roi.service.RoiService;
import com.lowagie.text.DocumentException;

/**
 * RESTful API controller for ROI calculations. Provides endpoints for returning
 * ROI metrics for visualization and PDF report generation.
 */
@RestController
@RequestMapping("/api/roi")
public class RoiApiController {

    @Autowired
    private RoiService roiService;
    @Autowired
    private RoiPdfReportService roiPdfReportService;

    /**
     * Calculate ROI with aggregated metrics for visualization
     *
     * @param request Contains battery size, usage, solar size, and other user inputs
     * @return RoiCalculationResponseWithPdfFlat object with various ROI metrics and pdfUrl (always null)
     */
 
    @PostMapping("/calculate")
    public ResponseEntity<RoiCalculationResponse> calculateRoi(@RequestBody RoiRequest request) {
        RoiCalculationResponse response = roiService.calculate(request);
    
        return ResponseEntity.ok(response);
    }


    /**
     * Generate a PDF report from a previously calculated ROI response
     *
     * @param response The ROI calculation response to report on
     * @return PdfReportLinkResponse containing the URL to the generated PDF
     */
    @PostMapping("/report")
    public ResponseEntity<PdfReportLinkResponse> generateReport(@RequestBody RoiCalculationResponse response) throws IOException, DocumentException {
        String reportId = UUID.randomUUID().toString();
        roiPdfReportService.generateRoiReportToFile(response, reportId);
        String pdfUrl = "/api/roi/reports/roi-report-" + reportId + ".pdf";
        return ResponseEntity.ok(new PdfReportLinkResponse(pdfUrl));
    }

    /**
     * GET endpoint for quickly viewing ROI data for debugging with default parameters
     */
    @GetMapping("/timeseries")
    public ResponseEntity<RoiCalculationResponse> getTimeSeriesData(
            @RequestParam(defaultValue = "17.5") double batterySize,
            @RequestParam(defaultValue = "4000") double usage,
            @RequestParam(defaultValue = "4.0") double solarSize) {

        // Note: This endpoint ignores the newer request parameters like direction, EV, etc.
        // It only uses the basic parameters for a quick check.
        RoiRequest request = new RoiRequest();
        request.setBatterySize(batterySize);
        request.setUsage(usage);
        request.setSolarSize(solarSize);

        try {
            RoiCalculationResponse response = roiService.calculate(request);
            if (response == null) {
                return ResponseEntity.internalServerError().build();
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // logger.error("Error during ROI calculation (GET endpoint)", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Endpoint to serve generated PDF reports from /tmp
     */
    @GetMapping("/reports/{filename:.+}")
    public ResponseEntity<Resource> getReport(@PathVariable String filename) throws IOException {
        Path file = Paths.get("/tmp").resolve(filename);
        Resource resource = new UrlResource(file.toUri());
        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(resource);
    }
}
