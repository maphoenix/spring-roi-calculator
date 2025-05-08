package com.example.roi.model;

public class RoiCalculationResponseWithPdf {
    private final RoiCalculationResponse calculationResponse;
    private final String pdfUrl;

    public RoiCalculationResponseWithPdf(RoiCalculationResponse calculationResponse, String pdfUrl) {
        this.calculationResponse = calculationResponse;
        this.pdfUrl = pdfUrl;
    }

    public RoiCalculationResponse getCalculationResponse() {
        return calculationResponse;
    }

    public String getPdfUrl() {
        return pdfUrl;
    }
} 