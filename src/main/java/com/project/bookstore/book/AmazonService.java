package com.project.bookstore.book;

import io.github.bonigarcia.wdm.WebDriverManager;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.net.URLEncoder;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;


@Service
public class AmazonService {
    private WebDriver driver;
    private final Logger log = LoggerFactory.getLogger(getClass());
    private static final String AMAZON_DP_URL = "https://www.amazon.com/dp/";

    @PostConstruct
    public void initialize() {
        try {
            WebDriverManager.chromedriver().setup();
            ChromeOptions options = new ChromeOptions()
                    .addArguments("--headless")
                    .addArguments("user‑agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                    .addArguments("--disable-blink-features=AutomationControlled");
            driver = new ChromeDriver(options);
        } catch (Exception e) {
            log.warn("Could not initialize ChromeDriver – Amazon searches will be disabled", e);
            driver = null;
        }
    }

    public List<AmazonBookDto> searchAmazonBooks(String query) {
        if (driver == null) {
            return Collections.emptyList();
        }
        List<AmazonBookDto> results = new ArrayList<>();
        try {
            String url = "https://www.amazon.com/s?k=" + URLEncoder.encode(query, "UTF-8") + "&i=stripbooks";
            System.out.println("Request URL: " + url);

            driver.get(url);

            // Use retry logic to wait for the main slot to load
            if (!waitForMainSlot(driver, 3)) {
                System.out.println("Failed to load main content after retries.");
                return results;
            }

            // Give additional time for dynamic content to render
            Thread.sleep(3000);

            System.out.println("Page title: " + driver.getTitle());

            List<WebElement> bookItems = driver.findElements(
                    By.cssSelector("div.s-main-slot div[data-component-type='s-search-result']")
            );
            System.out.println("Found " + bookItems.size() + " book items");

            for (WebElement book : bookItems) {
                try {
                    String bookId = book.getAttribute("data-asin");
                    System.out.println("Processing book with ASIN: " + bookId);

                    // Extract title (using dynamic selectors)
                    String title = "N/A";
                    try {
                        List<WebElement> h2Elements = book.findElements(By.tagName("h2"));
                        for (WebElement h2 : h2Elements) {
                            String ariaLabel = h2.getAttribute("aria-label");
                            if (ariaLabel != null && !ariaLabel.isEmpty()) {
                                title = ariaLabel;
                                break;
                            }
                            try {
                                String spanText = h2.findElement(By.cssSelector("span.a-size-medium.a-color-base.a-text-normal")).getText();
                                if (spanText != null && !spanText.isEmpty()) {
                                    title = spanText;
                                    break;
                                }
                            } catch (Exception e) {
                                try {
                                    String spanText = h2.findElement(By.tagName("span")).getText();
                                    if (spanText != null && !spanText.isEmpty()) {
                                        title = spanText;
                                        break;
                                    }
                                } catch (Exception ex) {
                                    // Continue checking next h2 element
                                }
                            }
                        }
                    } catch (Exception e) {
                        System.out.println("Failed to extract title: " + e.getMessage());
                    }

                    // AUTHOR extraction (similar as before)
                    String author = "N/A";
                    try {
                        List<WebElement> authorLinks = book.findElements(
                                By.cssSelector("a.a-size-base.a-link-normal.s-underline-text")
                        );
                        if (!authorLinks.isEmpty()) {
                            author = authorLinks.get(0).getText();
                        } else {
                            authorLinks = book.findElements(By.cssSelector(".a-row a"));
                            for (WebElement link : authorLinks) {
                                String text = link.getText();
                                if (text != null && !text.isEmpty() &&
                                        !text.contains("$") && !text.contains("stars")) {
                                    author = text;
                                    break;
                                }
                            }
                        }
                    } catch (Exception e) {
                        System.out.println("Failed to extract author: " + e.getMessage());
                    }

                    // PRICE extraction
                    String price = "N/A";
                    try {
                        List<WebElement> priceElements = book.findElements(By.cssSelector(".a-price"));
                        if (!priceElements.isEmpty()) {
                            WebElement priceElement = priceElements.get(0);
                            try {
                                String wholePart = priceElement.findElement(By.cssSelector(".a-price-whole")).getText();
                                String fractionPart = priceElement.findElement(By.cssSelector(".a-price-fraction")).getText();
                                price = "$" + wholePart + "." + fractionPart;
                            } catch (Exception e) {
                                price = priceElement.getText().replace("\n", ".");
                            }
                        }
                    } catch (Exception e) {
                        System.out.println("Failed to extract price: " + e.getMessage());
                    }

                    // IMAGE extraction
                    String imageUrl = "/placeholder.jpg";
                    try {
                        WebElement imgElement = book.findElement(By.cssSelector("img.s-image"));
                        imageUrl = imgElement.getAttribute("src");
                    } catch (Exception e) {
                        System.out.println("Failed to extract image URL: " + e.getMessage());
                    }

                    System.out.println("Title: " + title);
                    System.out.println("Author: " + author);
                    System.out.println("Price: " + price);
                    System.out.println("Image URL: " + imageUrl);
                    System.out.println("------------------------------------------------");

                    if (title != null && !title.isEmpty() && !title.equals("N/A")) {
                        AmazonBookDto dto = new AmazonBookDto(bookId, title, author, price, imageUrl);
                        results.add(dto);
                    }

                } catch (Exception e) {
                    System.out.println("Error processing book item: " + e.getMessage());
                    e.printStackTrace();
                }
            }

            System.out.println("Successfully processed " + results.size() + " books");

        } catch (Exception e) {
            System.out.println("General error in searchAmazonBooks: " + e.getMessage());
            e.printStackTrace();
        }
        return results;
    }


    @PreDestroy
    public void cleanup() {
        if (driver != null) {
            driver.quit();
        }
    }

    private boolean waitForMainSlot(WebDriver driver, int maxRetries) {
        int attempts = 0;
        while (attempts < maxRetries) {
            try {
                WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
                wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("div.s-main-slot")));
                return true;
            } catch (Exception e) {
                System.out.println("Attempt " + (attempts + 1) + " failed. Retrying...");
                attempts++;
                driver.navigate().refresh();
                try {
                    Thread.sleep(5000); // Wait additional time after refresh
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        return false;
    }

    public String findCoverUrlById(String asin) {
        try {
            // use Jsoup (or your Selenium code) to fetch the page
            Document doc = Jsoup.connect(AMAZON_DP_URL + asin)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                    .timeout(10_000)
                    .get();

            // try the “landing image” selector first
            Element img = doc.selectFirst("#imgBlkFront, .imgTagWrapper img");
            if (img != null) {
                String src = img.attr("src");
                if (!src.isEmpty()) return src;
            }
        } catch (Exception e) {
            // fall through
        }
        return null;
    }
}