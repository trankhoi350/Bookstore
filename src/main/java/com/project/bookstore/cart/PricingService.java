package com.project.bookstore.cart;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Service
public class PricingService {
    private static final BigDecimal BASELINE_PRICE = new BigDecimal("20.00");

    public BigDecimal determinePrice(Integer publicationYear, Integer pageCount, String genre) {
        BigDecimal multiplier = BigDecimal.ONE;

        // Adjust multiplier based on publication year
        if (publicationYear != null) {
            int currentYear = LocalDate.now().getYear();
            int age = currentYear - publicationYear;
            if (age <= 5) { // Recent books: premium +10%
                multiplier = multiplier.add(new BigDecimal("0.10"));
            } else if (age >= 20) { // Older books: discount -10%
                multiplier = multiplier.subtract(new BigDecimal("0.10"));
            }
        }

        // Adjust multiplier based on page count
        if (pageCount != null) {
            if (pageCount > 300) {
                multiplier = multiplier.add(new BigDecimal("0.05")); // +5% premium
            } else if (pageCount < 100) {
                multiplier = multiplier.subtract(new BigDecimal("0.05")); // -5% discount
            }
        }

        // Adjust multiplier based on genre
        if (genre != null) {
            switch (genre.toLowerCase()) {
                case "technical":
                case "academic":
                    multiplier = multiplier.add(new BigDecimal("0.15")); // +15% premium
                    break;
                case "fiction":
                    // No adjustment, or a slight discount if desired
                    break;
                case "children":
                    multiplier = multiplier.subtract(new BigDecimal("0.10")); // -10% discount
                    break;
                default:
                    // No adjustment for other genres
                    break;
            }
        }

        // Ensure multiplier does not drop below a minimum threshold (e.g., 0.8) or exceed a maximum (e.g., 1.5)
        if (multiplier.compareTo(new BigDecimal("0.8")) < 0) {
            multiplier = new BigDecimal("0.8");
        } else if (multiplier.compareTo(new BigDecimal("1.5")) > 0) {
            multiplier = new BigDecimal("1.5");
        }

        // Calculate final price
        return BASELINE_PRICE.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
    }

//    public BigDecimal calculatePrice(Integer publicationYear, String genre) {
//
//    }

    public Integer extractYear(String publishedDate) {
        try {
            // Split the string by non-digit characters and take the first 4-digit number
            String[] parts = publishedDate.split("[^0-9]+");
            if (parts.length > 0 && parts[0].length() >= 4) {
                return Integer.parseInt(parts[0].substring(0, 4));
            }
        } catch (Exception e) {
            // log error if needed
        }
        return null;
    }
}
