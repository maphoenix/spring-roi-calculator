package com.example.scrapers;

public class WebScraperEcoSupermarket {

    public void scrape (DefaultSystemInstallationCosts costs) throws Exception {
        /*
        // Example: Scrape from a public site (adjust URL and selectors as needed)
        String url = "https://www.theecoexperts.co.uk/solar-panels/cost";
        Document doc = Jsoup.connect(url).get();

        // Example: Find a table with system sizes and costs (adjust selector as needed)
        Elements rows = doc.select("table tr");

        // Fallback: If you can't scrape, use hardcoded or previously scraped data
        Map<Double, Integer> panelCostMap = new HashMap<>();
        // Example: Fill with scraped or hardcoded values
        // panelCostMap.put(4.0, 3600); // etc.

        // If you can scrape, parse the table
        for (Element row : rows) {
            Elements cols = row.select("td");
            if (cols.size() >= 2) {
                try {
                    double size = Double.parseDouble(cols.get(0).text().replaceAll("[^\\d.]", ""));
                    int cost = Integer.parseInt(cols.get(1).text().replaceAll("[^\\d]", ""));
                    panelCostMap.put(size, cost);
                } catch (Exception ignored) {}
            }
        }

        // If scraping fails, use fallback cost assumptions
       

        double[] blockSizes = {2.5, 3.6};
        double batteryCostPerKwh = 500;
        double batteryInstallCost = 400;
        double batteryInstallCostWithHybrid = 200;
        int batteryLifespan = 10;
        int batteryWarranty = 10;
        double usableCapacityFraction = 0.9;
        double maxBatteryKwh = 15.0;

        List<Map<String, Object>> systems = new ArrayList<>();

        for (double size = 2.0; size <= 40.0; size += 0.5) {
            Map<String, Object> system = new LinkedHashMap<>();
            system.put("system_size_kw", size);
            system.put("inverter_size_kw", size);

            Map<String, Object> install = new LinkedHashMap<>();
            int panelCost = panelCostMap.getOrDefault(size, (int) (costs.panelCostPerKw * size));


            install.put("panel_hardware_cost_gbp", costs.getPanelCostPerKw());
            install.put("hybrid_inverter_cost_gbp", (int) (costs.getHybridInverterCostPerKw() * size));
            install.put("mounting_system_cost_gbp", costs.mountingSystemCost);
            install.put("wiring_and_electrical_cost_gbp", costs.wiringCost);
            install.put("scaffolding_cost_gbp", costs.scaffoldingCost);
            install.put("labour_cost_gbp", costs.labourCost);
            install.put("mcs_registration_cost_gbp", costs.mcsCost);
            install.put("miscellaneous_cost_gbp", costs.miscCost);

            double totalCost = panelCost 
                 + (costs.hybridInverterCostPerKw * size)
                 + costs.mountingSystemCost 
                 + costs.wiringCost 
                 + costs.scaffoldingCost 
                 + costs.labourCost 
                 + costs.mcsCost 
                 + costs.miscCost;
            install.put("total_cost_gbp", totalCost);
            install.put("panel_lifespan_years", 25);
            install.put("panel_brand_quality", "Good");

            // Battery options
            List<Map<String, Object>> batteryOptions = new ArrayList<>();
            // 0 kWh option
            Map<String, Object> noBattery = new LinkedHashMap<>();
            noBattery.put("battery_size_kwh", 0);
            noBattery.put("usable_capacity_kwh", 0);
            noBattery.put("block_size_kwh", 0);
            noBattery.put("block_count", 0);
            noBattery.put("battery_hardware_cost_gbp", 0);
            noBattery.put("battery_installation_cost_with_hybrid_gbp", 0);
            noBattery.put("battery_installation_cost_without_hybrid_gbp", 0);
            noBattery.put("total_battery_cost_with_hybrid_gbp", 0);
            noBattery.put("total_battery_cost_without_hybrid_gbp", 0);
            noBattery.put("battery_lifespan_years", 0);
            noBattery.put("battery_warranty_years", 0);
            batteryOptions.add(noBattery);

            // Modular battery options
            for (double block : blockSizes) {
                for (int count = 1; count * block <= maxBatteryKwh; count++) {
                    double batterySize = block * count;
                    double usable = batterySize * usableCapacityFraction;
                    int hardwareCost = (int) (batteryCostPerKwh * batterySize);
                    int installWithHybrid = (int) batteryInstallCostWithHybrid;
                    int installWithoutHybrid = (int) batteryInstallCost;
                    int totalWithHybrid = hardwareCost + installWithHybrid;
                    int totalWithoutHybrid = hardwareCost + installWithoutHybrid;

                    Map<String, Object> battery = new LinkedHashMap<>();
                    battery.put("battery_size_kwh", batterySize);
                    battery.put("usable_capacity_kwh", usable);
                    battery.put("block_size_kwh", block);
                    battery.put("block_count", count);
                    battery.put("battery_hardware_cost_gbp", hardwareCost);
                    battery.put("battery_installation_cost_with_hybrid_gbp", installWithHybrid);
                    battery.put("battery_installation_cost_without_hybrid_gbp", installWithoutHybrid);
                    battery.put("total_battery_cost_with_hybrid_gbp", totalWithHybrid);
                    battery.put("total_battery_cost_without_hybrid_gbp", totalWithoutHybrid);
                    battery.put("battery_lifespan_years", batteryLifespan);
                    battery.put("battery_warranty_years", batteryWarranty);
                    batteryOptions.add(battery);
                }
            }
            install.put("battery_options", batteryOptions);
            system.put("installation", install);

            systems.add(system);
        }
        */
    }
}