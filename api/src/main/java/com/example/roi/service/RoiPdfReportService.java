package com.example.roi.service;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;

import javax.imageio.ImageIO;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.springframework.stereotype.Service;

import com.example.roi.model.RoiCalculationResponse;
import com.example.roi.model.RoiChartDataPoint;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
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

        // Installation Cost Section
        addInstallationCostSection(document, response);

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

    private void addInstallationCostSection(Document document, RoiCalculationResponse response) throws DocumentException {
        document.add(new Paragraph("Installation Cost Assumptions", new Font(Font.HELVETICA, 14, Font.BOLD, new Color(34, 139, 34))));
        if (response.getTotalCost() != null) {
            document.add(new Paragraph(String.format("- Assumed total installation cost: £%.2f %s",
                response.getTotalCost().getAmount(),
                response.getTotalCost().getCurrency() != null ? response.getTotalCost().getCurrency() : "GBP"
            )));
        } else {
            document.add(new Paragraph("- Installation cost data not available."));
        }
        document.add(new Paragraph(" "));
    }

    private void addInputSummary(Document document, RoiCalculationResponse response) throws DocumentException {
        document.add(new Paragraph("Input Summary", new Font(Font.HELVETICA, 16, Font.BOLD, new Color(34, 139, 34))));
        // TODO: Add a table or list of all input values from response
        document.add(new Paragraph(" "));
    }

    private void addCalculationBreakdown(Document document, RoiCalculationResponse response) throws DocumentException {
        document.add(new Paragraph("Calculation Breakdown (Yearly Table)", new Font(Font.HELVETICA, 16, Font.BOLD, new Color(34, 139, 34))));
        document.add(new Paragraph(" "));
        if (response.getYearlyBreakdown() != null && !response.getYearlyBreakdown().isEmpty()) {
            PdfPTable table = new PdfPTable(10);
            table.setWidthPercentage(100);
            String[] headers = {
                "Year", "Usable Battery (kWh)", "Degradation", "Shiftable (kWh)", "Battery Savings (£)",
                "Solar Used (kWh)", "Solar Export (kWh)", "Solar Savings (£)", "Yearly Total (£)", "Costs Outstanding (£)"
            };
            Font headerFont = new Font(Font.HELVETICA, 10, Font.BOLD, new Color(34, 139, 34));
            Font cellFont = new Font(Font.HELVETICA, 9);
            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Paragraph(header, headerFont));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setBackgroundColor(new Color(220, 255, 220));
                table.addCell(cell);
            }
            for (var yb : response.getYearlyBreakdown()) {
                table.addCell(new Paragraph(String.valueOf(yb.getYear()), cellFont));
                table.addCell(new Paragraph(String.format("%.2f", yb.getUsableBatteryMaxCapacity()), cellFont));
                table.addCell(new Paragraph(String.format("%.2f", yb.getDegradationFactor()), cellFont));
                table.addCell(new Paragraph(String.format("%.2f", yb.getShiftable()), cellFont));
                table.addCell(new Paragraph(String.format("£%d", Math.round(yb.getBatterySavings())), cellFont));
                table.addCell(new Paragraph(String.format("%.2f", yb.getSolarUsed()), cellFont));
                table.addCell(new Paragraph(String.format("%.2f", yb.getSolarExport()), cellFont));
                double solarTotal = yb.getSolarUsed() + yb.getSolarExport();
                int usedPct = solarTotal > 0 ? (int)Math.round(100.0 * yb.getSolarUsed() / solarTotal) : 0;
                int exportPct = solarTotal > 0 ? (int)Math.round(100.0 * yb.getSolarExport() / solarTotal) : 0;
                table.addCell(new Paragraph(String.format("£%d", Math.round(yb.getSolarSavingsSelfUse() + yb.getSolarSavingsExport())), cellFont));
                table.addCell(new Paragraph(String.format("£%d", Math.round(yb.getYearlyTotalSavings())), cellFont));
                table.addCell(new Paragraph(String.format("£%d", Math.round(yb.getCumulativeSavings())), cellFont));
            }
            document.add(table);
            document.add(new Paragraph("Note: All cost values in this table are rounded to the nearest pound.", new Font(Font.HELVETICA, 7, Font.ITALIC, Color.DARK_GRAY)));
        } else {
            document.add(new Paragraph("No yearly breakdown data available."));
        }
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
        document.add(new Paragraph(String.format("  Year: %d", year)));
        document.add(new Paragraph(String.format("  Costs Outstanding: £%.2f", dataPoint.getCumulativeSavings())));
        // Add detailed working out for Year 1 if available
        if (year == 1 && response.getYearlyBreakdown() != null && !response.getYearlyBreakdown().isEmpty()) {
            var yb = response.getYearlyBreakdown().get(0);
            document.add(new Paragraph(String.format("    Costs Outstanding: £%d", Math.round(yb.getCumulativeSavings()))));
            // Add blank line before working out
            document.add(new Paragraph(" "));
            // 'Working Out (Year 1):' in regular font size (not bold)
            document.add(new Paragraph("  Working Out (Year 1):", new Font(Font.HELVETICA, 10)));
            // Data lines
            document.add(new Paragraph(String.format("    Usable Battery Max Capacity: %.2f kWh", yb.getUsableBatteryMaxCapacity())));
            document.add(new Paragraph(String.format("    Degradation Factor: %.2f", yb.getDegradationFactor())));
            document.add(new Paragraph(String.format("    Shiftable: %.2f kWh", yb.getShiftable())));
            double solarTotal = yb.getSolarUsed() + yb.getSolarExport();
            int usedPct = solarTotal > 0 ? (int)Math.round(100.0 * yb.getSolarUsed() / solarTotal) : 0;
            int exportPct = solarTotal > 0 ? (int)Math.round(100.0 * yb.getSolarExport() / solarTotal) : 0;
            document.add(new Paragraph(String.format("    Solar Used: %.2f kWh (%d%%)", yb.getSolarUsed(), usedPct)));
            document.add(new Paragraph(String.format("    Solar Export: %.2f kWh (%d%%)", yb.getSolarExport(), exportPct)));
            // Add blank line for clarity
            document.add(new Paragraph(" "));
            // Savings lines
            document.add(new Paragraph(String.format("    Battery Savings: £%d", Math.round(yb.getBatterySavings()))));
            document.add(new Paragraph(String.format("    Solar Savings (self-use): £%d", Math.round(yb.getSolarSavingsSelfUse()))));
            document.add(new Paragraph(String.format("    Solar Savings (export): £%d", Math.round(yb.getSolarSavingsExport()))));
            document.add(new Paragraph(String.format("    Costs Outstanding: £%d", Math.round(yb.getCumulativeSavings()))));
            // Total savings in regular font size (12pt, not bold)
            document.add(new Paragraph(String.format("    Yearly Total Savings: £%d", Math.round(yb.getYearlyTotalSavings())), new Font(Font.HELVETICA, 12)));
        }
        // TODO: Add more detailed breakdown if available (battery savings, solar savings, etc.)
    }

    private void addCharts(Document document, RoiCalculationResponse response) throws IOException, DocumentException {
        document.add(new Paragraph("Cumulative Savings Chart", new Font(Font.HELVETICA, 16, Font.BOLD, new Color(34, 139, 34))));
        document.add(new Paragraph(" "));
        if (response.getYearlyBreakdown() != null && !response.getYearlyBreakdown().isEmpty()) {
            DefaultCategoryDataset dataset = new DefaultCategoryDataset();
            for (var yb : response.getYearlyBreakdown()) {
                dataset.addValue(yb.getCumulativeSavings(), "Cumulative Savings", String.valueOf(yb.getYear()));
            }
            JFreeChart chart = ChartFactory.createLineChart(
                "Cumulative Savings Over Time",
                "Year",
                "Cumulative Savings (£)",
                dataset,
                PlotOrientation.VERTICAL,
                false, true, false
            );
            BufferedImage chartImage = chart.createBufferedImage(600, 300);
            java.io.ByteArrayOutputStream chartBaos = new java.io.ByteArrayOutputStream();
            ImageIO.write(chartImage, "png", chartBaos);
            Image chartPdfImage = Image.getInstance(chartBaos.toByteArray());
            chartPdfImage.setAlignment(Element.ALIGN_CENTER);
            document.add(chartPdfImage);
        } else {
            document.add(new Paragraph("No chart data available."));
        }
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