package com.example.roi.model;

public class PdfReportLinkResponse {
    private final String pdfUrl;

    public PdfReportLinkResponse(String pdfUrl) {
        this.pdfUrl = pdfUrl;
    }

    public String getPdfUrl() {
        return pdfUrl;
    }
} 