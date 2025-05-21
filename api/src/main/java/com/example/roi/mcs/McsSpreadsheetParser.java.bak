package com.example.roi.mcs;

import java.io.FileInputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * NOTE: This parser is designed for the MCS self-consumption lookup spreadsheet.
 * Each data set contains:
 * - A header cell with occupancy type and consumption range
 * - 15 columns of battery size data (plus 1 column for PV generation description)
 * - 20 rows of percentage values
 * Sets are arranged both horizontally (separated by blank columns) and vertically (separated by blank rows)
 */
public class McsSpreadsheetParser {
    private static final int BATTERY_COLUMNS = 15; // Number of battery size columns
    private static final int PV_ROWS = 20;        // Number of PV generation rows
    private static final Pattern PV_RANGE_PATTERN = Pattern.compile("(\\d+(?:,\\d+)?)[\\s]*(?:kWh|kwh)[\\s]*to[\\s]*(\\d+(?:,\\d+)?)", Pattern.CASE_INSENSITIVE);
    private static final DataFormatter DATA_FORMATTER = new DataFormatter();

    public enum OccupancyType {
        HOME_ALL_DAY("Home all day", "home_all_day"),
        OUT_DURING_DAY("Out during the day", "out_during_day"),
        IN_HALF_DAY("In half the day", "in_half_day");

        private final String displayName;
        private final String jsonKey;

        OccupancyType(String displayName, String jsonKey) {
            this.displayName = displayName;
            this.jsonKey = jsonKey;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getJsonKey() {
            return jsonKey;
        }

        public static OccupancyType fromDisplayName(String name) {
            System.out.println("Attempting to match occupancy type: '" + name + "'");
            for (OccupancyType type : values()) {
                if (type.displayName.equalsIgnoreCase(name.trim())) {
                    System.out.println("Successfully matched to: " + type.name());
                    return type;
                }
            }
            System.err.println("WARNING: No match found for occupancy type: '" + name + "'");
            throw new IllegalArgumentException("Unknown occupancy type: " + name);
        }
    }

    private static class PvBand {
        String pvGenerationRange;  // For sorting and display
        int pvMin;  // Minimum PV generation
        int pvMax;  // Maximum PV generation
        Map<String, Double> fractionsByBatteryKwh = new TreeMap<>((a, b) -> {
            double aVal = Double.parseDouble(a);
            double bVal = Double.parseDouble(b);
            return Double.compare(aVal, bVal);
        });
    }

    private static void setGenerationRange(PvBand band, double dailyKwh) {
        // Each range covers 300 kWh
        int rangeStart = ((int)dailyKwh / 300) * 300;  // Round down to nearest 300
        int rangeEnd = rangeStart + 299;
        band.pvMin = rangeStart;
        band.pvMax = rangeEnd;
        band.pvGenerationRange = String.format("%d-%d", rangeStart, rangeEnd);
    }

    private static String formatConsumptionLabel(String range) {
        String[] parts = range.split("-");
        String start = String.format("%,d", Integer.parseInt(parts[0]));
        String end = String.format("%,d", Integer.parseInt(parts[1]));
        return "Annual consumption: " + start + " kWh to " + end + " kWh";
    }

    public static void main(String[] args) throws Exception {
        String excel = "src/main/resources/mcs/MGD003-LookupTables-FINAL (3).xlsx";
        String jsonOut = "mcs_self_consumption.json";
        
        System.out.println("Opening Excel file: " + excel);
        Workbook wb = new XSSFWorkbook(new FileInputStream(excel));
        Sheet sh = wb.getSheet("Lookup Tables");
        if (sh == null) {
            System.err.println("Could not find 'Lookup Tables' sheet!");
            return;
        }
        
        ObjectMapper M = new ObjectMapper();
        
        // Initialize data structures for each occupancy type
        Map<OccupancyType, ObjectNode> occupancyNodes = new TreeMap<>();
        Map<OccupancyType, ArrayNode> consumptionArrays = new TreeMap<>();

        // Initialize arrays for each occupancy type
        for (OccupancyType type : OccupancyType.values()) {
            ObjectNode node = M.createObjectNode();
            ArrayNode consumptionArray = M.createArrayNode();
            occupancyNodes.put(type, node);
            consumptionArrays.put(type, consumptionArray);
        }

        // Find all header cells
        List<CellAddress> headers = new ArrayList<>();
        for (Row row : sh) {
            if (row == null) continue;
            for (Cell cell : row) {
                if (cell != null && cell.getCellType() == CellType.STRING) {
                    String txt = cell.getStringCellValue();
                    if (txt.contains("Annual electricity consumption:") || 
                        (txt.contains("PV Only") && txt.contains("consumption:"))) {
                        headers.add(cell.getAddress());
                        System.out.println("\nFound header at " + cell.getAddress());
                        System.out.println("Header text: " + txt);
                    }
                }
            }
        }
        System.out.println("\nTotal headers found: " + headers.size());

        for (CellAddress addr : headers) {
            int r = addr.getRow(), c = addr.getColumn();
            Row headerRow = sh.getRow(r);
            if (headerRow == null) continue;
            
            Cell headerCell = headerRow.getCell(c);
            if (headerCell == null) continue;
            
            String header = headerCell.getStringCellValue().trim();
            String occ = null, cons;
            
            // Extract occupancy type from header
            if (header.contains("Occupancy:")) {
                occ = header.split("Occupancy:")[1].split("\\.")[0].trim();
                System.out.println("Found occupancy in header: '" + occ + "'");
            } else if (header.contains("Home all day")) {
                occ = "Home all day";
                System.out.println("Found occupancy in header: '" + occ + "'");
            } else if (header.contains("Out during the day")) {
                occ = "Out during the day";
                System.out.println("Found occupancy in header: '" + occ + "'");
            } else if (header.contains("In half the day")) {
                occ = "In half the day";
                System.out.println("Found occupancy in header: '" + occ + "'");
            } else {
                System.out.println("No occupancy found in header: '" + header + "'");
                continue;  // Skip invalid headers
            }

            // Extract consumption range
            if (header.contains("consumption:")) {
                cons = header.split("consumption:")[1]
                        .replace(" kWh", "")
                        .trim()
                        .replace(",", "")
                        .replace(" to ", "-");
            } else {
                System.out.println("No consumption range found in header: '" + header + "'");
                continue;  // Skip invalid headers
            }

            String consLabel = formatConsumptionLabel(cons);

            System.out.println("\nProcessing block at " + addr);
            System.out.println("Occupancy: " + occ);
            System.out.println("Consumption: " + consLabel);

            // Find battery sizes by scanning the next few rows for a row containing "kWh"
            List<String> batterySizes = Arrays.asList("0", "1.1", "2.1", "3.1", "4.1", "5.1", "6.1", "7.1", "8.1", "9.1", "10.1", "11.1", "12.1", "13.1", "14.1");
            List<PvBand> pvBands = new ArrayList<>();
            int batteryRow = -1;
            Row batRow = null;

            // Find the battery sizes row (should be 2 rows after the header)
            batRow = sh.getRow(r + 1);  // Changed from +2 to +1
            if (batRow != null) {
                batteryRow = r + 1;
            }

            // Process PV data rows (start 2 rows after battery sizes)
            int pvDataRow = batteryRow + 2;
            for (int rowIdx = pvDataRow; rowIdx < pvDataRow + PV_ROWS; rowIdx++) {
                Row row = sh.getRow(rowIdx);
                if (row != null) {
                    // Check for PV Only value in the first column
                    Cell firstCell = row.getCell(c - 1);
                    String firstCellText = firstCell != null ? DATA_FORMATTER.formatCellValue(firstCell).trim() : "";
                    System.out.println("Row " + rowIdx + " - First cell: '" + firstCellText + "'");

                    if (firstCellText.equals("PV Only")) {
                        System.out.println("Found PV Only row");
                        PvBand band = new PvBand();
                        try {
                            // Get the PV generation range from the current cell (c - 1 is "PV Only", c has the range)
                            Cell rangeCell = row.getCell(c);
                            if (rangeCell == null) {
                                System.err.println("Warning: Range cell is null for PV Only row " + rowIdx);
                                continue;
                            }

                            String rangeText = DATA_FORMATTER.formatCellValue(rangeCell).trim();
                            if (rangeText.isEmpty()) {
                                System.err.println("Warning: Empty range text for PV Only row " + rowIdx);
                                continue;
                            }
                            System.out.println("PV Only range text: '" + rangeText + "'");
                            
                            // Validate range format
                            if (!rangeText.toLowerCase().contains("kwh") || !rangeText.contains("to")) {
                                System.err.println("Warning: Invalid range format: " + rangeText);
                                continue;
                            }

                            // Get the percentage value
                            Cell percCell = row.getCell(c + 1);
                            if (percCell == null) {
                                System.err.println("Warning: Percentage cell is null for PV Only row " + rowIdx);
                                continue;
                            }

                            if (percCell.getCellType() != CellType.NUMERIC) {
                                System.err.println("Warning: Percentage cell is not numeric for PV Only row " + rowIdx);
                                continue;
                            }

                            double perc = percCell.getNumericCellValue();
                            System.out.println("PV Only percentage value: " + perc);
                            
                            // Validate percentage range
                            if (perc <= 0) {
                                System.err.println("Warning: Invalid percentage value: " + perc);
                                continue;
                            }

                            // Store the percentage value directly from the spreadsheet
                            System.out.println("PV Only final value: " + perc);
                            
                            // Parse the range values with validation
                            String[] parts = rangeText.split(" to ");
                            if (parts.length != 2) {
                                System.err.println("Warning: Invalid range format (split failed): " + rangeText);
                                continue;
                            }

                            try {
                                // Extract just the numbers, handling commas in the values
                                String minStr = parts[0].replaceAll("[^0-9]", "");
                                String maxStr = parts[1].replaceAll("[^0-9]", "");
                                
                                System.out.println("Range parsing debug:");
                                System.out.println("  Original range text: '" + rangeText + "'");
                                System.out.println("  Split parts: ['" + parts[0] + "', '" + parts[1] + "']");
                                System.out.println("  Extracted min string: '" + minStr + "'");
                                System.out.println("  Extracted max string: '" + maxStr + "'");

                                if (minStr.isEmpty() || maxStr.isEmpty()) {
                                    System.err.println("Warning: Failed to extract numbers from range: " + rangeText);
                                    continue;
                                }

                                // Parse the actual values from the range text
                                int pvMin = Integer.parseInt(minStr);
                                int pvMax = Integer.parseInt(maxStr);

                                System.out.println("  Parsed values:");
                                System.out.println("    pvMin: " + pvMin);
                                System.out.println("    pvMax: " + pvMax);

                                // Validate range values
                                if (pvMin < 0 || pvMax < 0 || pvMin >= pvMax) {
                                    System.err.println("Warning: Invalid range values: min=" + pvMin + ", max=" + pvMax);
                                    continue;
                                }

                                band.pvMin = pvMin;
                                band.pvMax = pvMax;
                                band.pvGenerationRange = String.format("%d-%d", pvMin, pvMax);
                                band.fractionsByBatteryKwh.put("0", perc);  // Only use battery size 0 for PV Only
                                pvBands.add(band);
                                System.out.println("  Final generation range: " + band.pvGenerationRange);
                                System.out.println("  Final percentage value: " + perc);
                                System.out.println("Successfully added PV band: " + band.pvGenerationRange + " with percentage: " + perc);
                            } catch (NumberFormatException e) {
                                System.err.println("Warning: Failed to parse range numbers: " + rangeText);
                                System.err.println("Error details: " + e.getMessage());
                            }
                        } catch (Exception e) {
                            System.err.println("Error processing PV Only row " + rowIdx + ": " + e.getMessage());
                            e.printStackTrace();
                        }
                    } else {
                        Cell pvCell = row.getCell(c);
                        if (pvCell != null) {
                            String pvText = DATA_FORMATTER.formatCellValue(pvCell).trim();
                            if (!pvText.isEmpty() && Character.isDigit(pvText.charAt(0))) {
                                PvBand band = new PvBand();
                                try {
                                    // Get the PV generation value from the first column
                                    Cell pvGenCell = row.getCell(c - 1);
                                    String pvValue = pvGenCell != null ? DATA_FORMATTER.formatCellValue(pvGenCell).trim() : "";
                                    System.out.println("Row " + rowIdx + " - PV value: '" + pvValue + "'");
                                    if (!pvValue.isEmpty()) {
                                        // Extract numbers from the range (e.g. "0 kWh to 299 kWh")
                                        String[] numbers = pvValue.split(" ")[0].split("-");
                                        double dailyKwh = Double.parseDouble(numbers[0]);
                                        setGenerationRange(band, dailyKwh);
                                        
                                        // Read percentages for each battery size
                                        for (int i = 0; i < batterySizes.size(); i++) {
                                            Cell percCell = row.getCell(c + i);
                                            if (percCell.getCellType() == CellType.NUMERIC) {
                                                double perc = percCell.getNumericCellValue();
                                                // Store the percentage value directly from the spreadsheet
                                                band.fractionsByBatteryKwh.put(batterySizes.get(i), perc);
                                            }
                                        }
                                        pvBands.add(band);
                                    }
                                } catch (Exception e) {
                                    System.err.println("Error processing row: " + e.getMessage());
                                }
                            }
                        }
                    }
                }
            }
            
            // Sort PV bands by the start of their generation range
            pvBands.sort((a, b) -> {
                int aStart = Integer.parseInt(a.pvGenerationRange.split("-")[0]);
                int bStart = Integer.parseInt(b.pvGenerationRange.split("-")[0]);
                return Integer.compare(aStart, bStart);
            });
            
            // Deduplicate PV bands with the same generation range
            List<PvBand> deduplicatedBands = new ArrayList<>();
            Map<String, PvBand> rangeMap = new TreeMap<>();
            
            for (PvBand band : pvBands) {
                if (rangeMap.containsKey(band.pvGenerationRange)) {
                    // Merge with existing band, keeping highest fraction for each battery size
                    PvBand existing = rangeMap.get(band.pvGenerationRange);
                    for (Map.Entry<String, Double> entry : band.fractionsByBatteryKwh.entrySet()) {
                        String batterySize = entry.getKey();
                        Double newFraction = entry.getValue();
                        Double existingFraction = existing.fractionsByBatteryKwh.get(batterySize);
                        if (existingFraction == null || newFraction > existingFraction) {
                            existing.fractionsByBatteryKwh.put(batterySize, newFraction);
                        }
                    }
                } else {
                    // Add new band to map
                    rangeMap.put(band.pvGenerationRange, band);
                    deduplicatedBands.add(band);
                }
            }
            
            // Create JSON output
            ObjectNode blockNode = M.createObjectNode();
            blockNode.put("occupancy", occ);
            blockNode.put("consumption", consLabel);
            
            ArrayNode batteryArray = M.createArrayNode();
            // Sort battery sizes numerically
            batterySizes.sort((a, b) -> Double.compare(Double.parseDouble(a), Double.parseDouble(b)));
            for (String batterySize : batterySizes) {
                batteryArray.add(batterySize);
            }
            blockNode.set("battery_sizes", batteryArray);
            
            ArrayNode pvBandsArray = M.createArrayNode();
            for (PvBand band : deduplicatedBands) {
                ObjectNode bandNode = M.createObjectNode();
                bandNode.put("pv_generation_range", band.pvGenerationRange);
                bandNode.put("pv_min", band.pvMin);
                bandNode.put("pv_max", band.pvMax);
                
                ArrayNode batteriesArray = M.createArrayNode();
                for (Map.Entry<String, Double> entry : band.fractionsByBatteryKwh.entrySet()) {
                    ObjectNode batteryNode = M.createObjectNode();
                    batteryNode.put("size", entry.getKey());
                    // Store the percentage value directly from the map
                    batteryNode.put("pv_generated_percentage", entry.getValue());
                    batteriesArray.add(batteryNode);
                }
                bandNode.set("batteries", batteriesArray);
                
                pvBandsArray.add(bandNode);
            }
            blockNode.set("bands", pvBandsArray);
            
            // Add to appropriate array based on occupancy type
            try {
                OccupancyType occupancyType = OccupancyType.fromDisplayName(occ);
                System.out.println("Processing block for occupancy type: " + occupancyType.name());
                
                ArrayNode consumptionArray = consumptionArrays.get(occupancyType);
                if (consumptionArray == null) {
                    System.err.println("WARNING: No consumption array found for occupancy type: " + occupancyType.name());
                    continue;
                }
                
                consumptionArray.add(blockNode);
                System.out.println("Added block to " + occupancyType.name() + " array. Current size: " + consumptionArray.size());
            } catch (IllegalArgumentException e) {
                System.err.println("Error processing block: " + e.getMessage());
            }
        }

        // Validate data before writing
        for (OccupancyType type : OccupancyType.values()) {
            ArrayNode array = consumptionArrays.get(type);
            System.out.println("Final validation - " + type.name() + " array size: " + array.size());
            if (array.size() == 0) {
                System.err.println("WARNING: No data found for occupancy type: " + type.name());
            }
        }
        
        // Create root node and add occupancy nodes
        ObjectNode root = M.createObjectNode();

        // Add occupancy nodes to root with proper structure
        for (OccupancyType type : OccupancyType.values()) {
            ObjectNode typeNode = occupancyNodes.get(type);
            ArrayNode consumptionArray = consumptionArrays.get(type);
            typeNode.set("consumption_ranges", consumptionArray);
            root.set(type.getJsonKey(), typeNode);
        }
        
        // Write JSON with pretty printing
        try (PrintWriter out = new PrintWriter(jsonOut)) {
            out.println(M.writerWithDefaultPrettyPrinter().writeValueAsString(root));
        }
        System.out.println("\nWrote JSON to: " + jsonOut);
        
        wb.close();
    }
}
