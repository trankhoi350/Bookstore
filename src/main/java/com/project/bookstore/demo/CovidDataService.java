package com.project.bookstore.demo;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class CovidDataService {

    private static final Logger logger = LoggerFactory.getLogger(CovidDataService.class);
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36";

    public List<CovidDataDto> retrieveCovidData() {
        List<CovidDataDto> covidDataList = new ArrayList<>();

        try {
            // Retrieving the desired web page with proper headers to avoid 403 Forbidden
            Document webPage = Jsoup
                    .connect("https://en.wikipedia.org/wiki/COVID-19_pandemic_by_country_and_territory")
                    .userAgent(USER_AGENT)
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                    .header("Referer", "https://www.google.com/")
                    .timeout(10000)
                    .get();

            logger.info("Successfully connected to Wikipedia");

            // Find the table containing COVID-19 data
            Element tableElement = findCovidTable(webPage);

            if (tableElement == null) {
                logger.error("Could not find COVID-19 data table on the page");
                return covidDataList;
            }

            logger.info("Found COVID-19 table: {}", tableElement.className());

            // Process table rows
            Elements rows = tableElement.select("tbody > tr");
            logger.info("Found {} rows in the table", rows.size());

            // Skip header rows - usually the first 1 or 2 rows
            int startRow = 0;
            for (int i = 0; i < rows.size(); i++) {
                Element row = rows.get(i);
                if (row.select("th").size() > 0 && row.select("th").text().contains("Country")) {
                    startRow = i + 1;
                    break;
                }
            }

            logger.info("Starting to process from row {}", startRow);

            // Process content rows
            for (int i = startRow; i < rows.size(); i++) {
                Element row = rows.get(i);

                try {
                    // Check if row has country and data cells
                    Elements countryElements = row.select("th > a, td > a");
                    Elements dataCells = row.select("td");

                    // Debug
                    logger.debug("Row {}: country elements: {}, data cells: {}",
                            i, countryElements.size(), dataCells.size());

                    if (countryElements.isEmpty() || dataCells.size() < 2) {
                        logger.debug("Skipping row {} - insufficient data", i);
                        continue;
                    }

                    // Extract country name - use the first link in the row
                    String country = countryElements.first().text().trim();

                    // Debug the current row we're processing
                    logger.debug("Processing row {}: Country found: '{}'", i, country);

                    // Skip rows with empty country names or footer rows
                    if (country.isEmpty() || country.contains("Total") || country.contains("World")) {
                        logger.debug("Skipping row with country: {}", country);
                        continue;
                    }

                    // Extract cases, deaths, and recoveries
                    // The layout might vary, so we'll log the cell contents to help debug
                    for (int j = 0; j < Math.min(5, dataCells.size()); j++) {
                        logger.debug("Cell {}: {}", j, dataCells.get(j).text());
                    }

                    // Try to extract cases, deaths, and recoveries
                    Integer cases = null;
                    Integer deaths = null;
                    Integer recoveries = null;

                    // Cases are typically in the first data cell
                    if (dataCells.size() > 0) {
                        cases = parseNumber(dataCells.get(0).text());
                    }

                    // Deaths are typically in the second data cell
                    if (dataCells.size() > 1) {
                        deaths = parseNumber(dataCells.get(1).text());
                    }

                    // Recoveries might be in the third data cell
                    if (dataCells.size() > 2) {
                        recoveries = parseNumber(dataCells.get(2).text());
                    }

                    // Create and add the data object
                    CovidDataDto dataDto = new CovidDataDto(country, cases, deaths, recoveries);
                    logger.debug("Created DTO: {}", dataDto);
                    covidDataList.add(dataDto);

                } catch (Exception e) {
                    logger.warn("Error processing row {}: {}", i, e.getMessage(), e);
                    // Continue with next row
                }
            }

            logger.info("Successfully extracted data for {} countries", covidDataList.size());

        } catch (IOException e) {
            logger.error("Error connecting to Wikipedia: " + e.getMessage(), e);
            throw new RuntimeException("Error connecting to Wikipedia", e);
        }

        return covidDataList;
    }

    private Element findCovidTable(Document document) {
        // Try different approaches to find the COVID-19 data table

        // First, try the most common tables that might contain COVID data
        String[] tableSelectors = {
                "table.wikitable.sortable",          // Common format for sortable tables in Wikipedia
                "table.wikitable",                   // General wikitable class
                "table.sortable",                    // Any sortable table
                "table#covid19-table",               // Specific ID that might be used
                "table.infobox",                     // Sometimes data is in infoboxes
                "table.mw-collapsible",              // Collapsible tables might contain the data
                "table"                              // Last resort: any table
        };

        // Look for tables that might contain COVID data
        for (String selector : tableSelectors) {
            Elements tables = document.select(selector);

            for (Element table : tables) {
                // Check if table headers suggest COVID data
                String tableText = table.text().toLowerCase();

                if ((tableText.contains("covid") || tableText.contains("coronavirus")) &&
                        tableText.contains("cases") && tableText.contains("deaths")) {

                    // Check for country column
                    boolean hasCountryColumn = false;

                    Elements headers = table.select("th");
                    for (Element header : headers) {
                        String headerText = header.text().toLowerCase();
                        if (headerText.contains("country") || headerText.contains("territory") ||
                                headerText.contains("location") || headerText.contains("region")) {
                            hasCountryColumn = true;
                            break;
                        }
                    }

                    if (hasCountryColumn) {
                        logger.info("Found COVID table with selector: {}", selector);
                        return table;
                    }
                }
            }
        }

        // If we haven't found a table yet, try a more specific approach
        // Look for the main COVID-19 statistics table which typically has a specific structure
        for (Element table : document.select("table")) {
            // These are common headers in COVID-19 data tables
            if (table.select("th:contains(Cases), th:contains(Deaths)").size() >= 2) {
                logger.info("Found table with cases and deaths columns");
                return table;
            }
        }

        logger.warn("Could not find any suitable COVID-19 data table");
        return null;
    }

    private Integer parseNumber(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }

        try {
            // Remove any non-digit characters except for decimal points
            String cleaned = text.replaceAll("[^0-9.]", "");

            // Handle decimal numbers by truncating
            if (cleaned.contains(".")) {
                double value = Double.parseDouble(cleaned);
                return (int) value;
            }

            // Parse integer
            if (cleaned.isEmpty()) {
                return null;
            }
            return Integer.parseInt(cleaned);
        } catch (NumberFormatException e) {
            logger.warn("Could not parse number from text: '{}'", text);
            return null;
        }
    }
}