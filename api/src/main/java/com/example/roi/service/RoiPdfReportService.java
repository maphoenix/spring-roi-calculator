package com.example.roi.service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;

import org.springframework.stereotype.Service;

import com.example.roi.model.RoiCalculationResponse;
import com.example.roi.model.RoiChartDataPoint;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;

@Service
public class RoiPdfReportService {

    /**
     * Generates a professional PDF ROI report for The Big Green Energy Company.
     * Includes branding, green theme, charts, formulas, and all calculation details.
     *
     * @param response The ROI calculation response to report on
     * @return PDF as a byte array (for download or email attachment)
     */
    public byte[] generateRoiReport(RoiCalculationResponse response) throws IOException, DocumentException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 50, 50, 50, 50);
        PdfWriter writer = PdfWriter.getInstance(document, baos);
        document.open();

        // Cover Page
        addCoverPage(document);

        // Assumptions & Constants
        addAssumptionsSection(document);

        // Input Summary
        addInputSummary(document, response);

        // Calculation Breakdown
        addCalculationBreakdown(document, response);

        // Worked Example (Year 1 ... Year 15)
        addWorkedExampleSection(document, response);

        // Charts
        addCharts(document, response);

        // Summary Table
        addSummarySection(document, response);

        // Formulas
        addFormulasSection(document);

        // Explanatory Notes
        addNotesSection(document);

        document.close();
        writer.close();
        return baos.toByteArray();
    }

    public String generateRoiReportToFile(RoiCalculationResponse response, String reportId) throws IOException, DocumentException {
        String filePath = "/tmp/roi-report-" + reportId + ".pdf";
        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            byte[] pdfBytes = generateRoiReport(response);
            fos.write(pdfBytes);
        }
        return filePath;
    }

    private void addCoverPage(Document document) throws DocumentException {
        Paragraph title = new Paragraph("The Big Green Energy Company\nROI Calculation Report", new Font(Font.HELVETICA, 24, Font.BOLD, new Color(34, 139, 34)));
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);
        document.add(new Paragraph("Date: " + LocalDate.now(), new Font(Font.HELVETICA, 12)));
        document.add(new Paragraph(" "));
    }

    private void addAssumptionsSection(Document document) throws DocumentException {
        document.add(new Paragraph("Assumptions & Constants", new Font(Font.HELVETICA, 16, Font.BOLD, new Color(34, 139, 34))));
        document.add(new Paragraph(" "));
        document.add(new Paragraph("- Battery efficiency: 85% (round-trip efficiency for battery storage)"));
        document.add(new Paragraph("- Usable battery percentage: 90% (portion of battery capacity that is usable)"));
        document.add(new Paragraph("- Battery degradation: 70% capacity after 10 years, linear decline to 0% at 15 years"));
        document.add(new Paragraph("- Maximum battery lifespan: 15 years"));
        document.add(new Paragraph("- Solar generation factor: 850 kWh/kW/year (typical UK value)"));
        document.add(new Paragraph("- Solar self-use percentage: 50% (if not home during day), 70% (if home during day)"));
        document.add(new Paragraph("- Solar export percentage: 50% (if not home during day), 30% (if home during day)"));
        document.add(new Paragraph("- Battery cost per kWh: £500.00"));
        document.add(new Paragraph("- Solar cost per kW: £1,500.00"));
        document.add(new Paragraph("- Tariff rates: User-selected or typical market rates for peak, off-peak, and export"));
        document.add(new Paragraph("- All calculations are based on the above constants and user-provided inputs."));
        document.add(new Paragraph(" "));
    }

    private void addInputSummary(Document document, RoiCalculationResponse response) throws DocumentException {
        document.add(new Paragraph("Input Summary", new Font(Font.HELVETICA, 16, Font.BOLD, new Color(34, 139, 34))));
        // TODO: Add a table or list of all input values from response
        document.add(new Paragraph(" "));
    }

    private void addCalculationBreakdown(Document document, RoiCalculationResponse response) throws DocumentException {
        document.add(new Paragraph("Calculation Breakdown", new Font(Font.HELVETICA, 16, Font.BOLD, new Color(34, 139, 34))));
        // TODO: Add a table with year-by-year breakdown
        document.add(new Paragraph(" "));
    }

    private void addWorkedExampleSection(Document document, RoiCalculationResponse response) throws DocumentException {
        document.add(new Paragraph("Worked Example: Year 1 ... Year 15", new Font(Font.HELVETICA, 16, Font.BOLD, new Color(34, 139, 34))));
        document.add(new Paragraph(" "));
        // For demonstration, we use the first and last data points from the chart data
        if (response.getRoiChartData() != null && response.getRoiChartData().getDataPoints() != null && !response.getRoiChartData().getDataPoints().isEmpty()) {
            var dataPoints = response.getRoiChartData().getDataPoints();
            RoiChartDataPoint year1 = dataPoints.get(0);
            RoiChartDataPoint year15 = dataPoints.get(dataPoints.size() - 1);

            // Year 1
            document.add(new Paragraph("Year 1:", new Font(Font.HELVETICA, 14, Font.BOLD)));
            addWorkedExampleYear(document, year1, 1, response);
            document.add(new Paragraph(" "));
            document.add(new Paragraph("...", new Font(Font.HELVETICA, 14, Font.BOLD)));
            document.add(new Paragraph(" "));
            // Year 15
            document.add(new Paragraph("Year 15:", new Font(Font.HELVETICA, 14, Font.BOLD)));
            addWorkedExampleYear(document, year15, 15, response);
            document.add(new Paragraph(" "));
        } else {
            document.add(new Paragraph("Worked example data is not available."));
        }
    }

    private void addWorkedExampleYear(Document document, RoiChartDataPoint dataPoint, int year, RoiCalculationResponse response) throws DocumentException {
        // In a real implementation, you would extract all relevant values for the year.
        // Here, we show the cumulative savings and year as an example.
        document.add(new Paragraph(String.format("  Year: %d", year)));
        document.add(new Paragraph(String.format("  Cumulative Savings: £%.2f", dataPoint.getCumulativeSavings())));
        // TODO: Add more detailed breakdown if available (battery savings, solar savings, etc.)
    }

    private void addCharts(Document document, RoiCalculationResponse response) throws IOException, DocumentException {
        // TODO: Generate and embed charts using JFreeChart
        document.add(new Paragraph("Charts", new Font(Font.HELVETICA, 16, Font.BOLD, new Color(34, 139, 34))));
        document.add(new Paragraph(" "));
    }

    private void addSummarySection(Document document, RoiCalculationResponse response) throws DocumentException {
        document.add(new Paragraph("Summary", new Font(Font.HELVETICA, 16, Font.BOLD, new Color(34, 139, 34))));
        // TODO: Add a summary table with key results
        document.add(new Paragraph(" "));
    }

    private void addFormulasSection(Document document) throws DocumentException {
        document.add(new Paragraph("How We Calculate Your Results", new Font(Font.HELVETICA, 16, Font.BOLD, new Color(34, 139, 34))));
        document.add(new Paragraph(" "));
        document.add(new Paragraph("Battery Savings (per year):", new Font(Font.HELVETICA, 12, Font.BOLD)));
        document.add(new Paragraph("  The lesser of (Usable Battery Max Capacity × Degradation Factor × 365) or Usage, multiplied by (Peak Rate minus Offpeak Rate), multiplied by Battery Efficiency."));
        document.add(new Paragraph(" "));
        document.add(new Paragraph("Solar Savings (per year):", new Font(Font.HELVETICA, 12, Font.BOLD)));
        document.add(new Paragraph("  (Solar Used × Peak Rate) plus (Solar Export × Export Rate)."));
        document.add(new Paragraph(" "));
        document.add(new Paragraph("Yearly Total Savings:", new Font(Font.HELVETICA, 12, Font.BOLD)));
        document.add(new Paragraph("  Battery Savings + Solar Savings."));
        document.add(new Paragraph(" "));
        document.add(new Paragraph("Cumulative Savings (per year):", new Font(Font.HELVETICA, 12, Font.BOLD)));
        document.add(new Paragraph("  The sum of Yearly Total Savings up to this year, minus the Initial Cost."));
        document.add(new Paragraph(" "));
        document.add(new Paragraph("Payback Period:", new Font(Font.HELVETICA, 12, Font.BOLD)));
        document.add(new Paragraph("  The first year when Cumulative Savings becomes greater than zero."));
        document.add(new Paragraph(" "));
        document.add(new Paragraph("ROI Percentage:", new Font(Font.HELVETICA, 12, Font.BOLD)));
        document.add(new Paragraph("  (Total Savings divided by Initial Cost) × 100."));
        document.add(new Paragraph(" "));
        document.add(new Paragraph("What the variables mean:", new Font(Font.HELVETICA, 12, Font.BOLD)));
        document.add(new Paragraph("  Usable Battery Max Capacity: Maximum usable battery capacity (kWh)"));
        document.add(new Paragraph("  Degradation Factor: Battery degradation for the year (e.g., 0.85)"));
        document.add(new Paragraph("  Usage: Annual energy usage (kWh)"));
        document.add(new Paragraph("  Peak Rate, Offpeak Rate, Export Rate: Tariff rates (GBP/kWh)"));
        document.add(new Paragraph("  Solar Used: Solar energy used on-site (kWh)"));
        document.add(new Paragraph("  Solar Export: Solar energy exported (kWh)"));
        document.add(new Paragraph("  Battery Efficiency: Battery round-trip efficiency (e.g., 0.85)"));
        document.add(new Paragraph("  Initial Cost: Upfront system cost (GBP)"));
        document.add(new Paragraph("  Total Savings: Cumulative savings at the end of the period (GBP)"));
        document.add(new Paragraph(" "));
    }

    private void addNotesSection(Document document) throws DocumentException {
        document.add(new Paragraph("Explanatory Notes", new Font(Font.HELVETICA, 16, Font.BOLD, new Color(34, 139, 34))));
        document.add(new Paragraph("This report details the calculations and assumptions used to estimate the return on investment for your solar and battery installation. For questions, contact our support team."));
        document.add(new Paragraph(" "));
    }
} 