package com.project.bookstore.book;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping(path = "api/bookstore")
@CrossOrigin(origins = "*")
public class BookController {
    private final BookService localBookService;
    private final GoogleBookService googleBookService;
    private final OpenLibraryService openLibraryService;
    private final AmazonService amazonService;



    @Autowired
    public BookController(BookService localBookService,
                          GoogleBookService googleBookService,
                          OpenLibraryService openLibraryService,
                          AmazonService amazonService) {
        this.localBookService = localBookService;
        this.googleBookService = googleBookService;
        this.openLibraryService = openLibraryService;
        this.amazonService = amazonService;
    }

    @GetMapping
    public List<Book> getBooks() {
        return localBookService.getBooks();
    }

    @GetMapping("/search")
    public BookSearchResponse search(@RequestParam String query, @RequestHeader("Authorization") String token) {
        // Log the search
        if (token != null && token.startsWith("Bearer ")) {
            localBookService.logSearch(query, token);
        }

        // A) First, search your own database
        List<Book> local = localBookService.fullTextSearch(query);

        // B) See if *any* local hit is an exact title match
        Optional<Book> exact = local.stream()
                .filter(b -> b.getTitle() != null && b.getTitle().equalsIgnoreCase(query))
                .findFirst();

        if (exact.isPresent()) {
            // As soon as we find *one* exact match, return "single"
            return new BookSearchResponse(exact.get());
        }

        // C) No exact local match → Use smartSearch for general term search
        List<UnifiedBookDto> unifiedResults = localBookService.smartSearch(query);

        // D) Convert UnifiedBookDto back to separate lists for BookSearchResponse
        List<Book> sortedLocal = new ArrayList<>();
        List<GoogleBookDto> sortedGoogle = new ArrayList<>();
        List<OpenLibraryBookDto> sortedOpen = new ArrayList<>();
        List<AmazonBookDto> sortedAmazon = new ArrayList<>();

        for (UnifiedBookDto unified : unifiedResults) {
            Book book = new Book();
            book.setTitle(unified.getTitle());
            book.setAuthor(unified.getAuthor());
            book.setImageUrl(unified.getImageUrl());
            // Handle price with null and "N/A" checks
            String priceStr = unified.getPrice();
            if (priceStr != null) {
                priceStr = priceStr.trim();
                if (!priceStr.equals("N/A") && !priceStr.isEmpty()) {
                    try {
                        book.setPrice(new BigDecimal(priceStr.replace("$", "").trim()));
                    } catch (NumberFormatException e) {
                        book.setPrice(null); // Set to null if parsing fails
                    }
                } else {
                    book.setPrice(null); // Set to null for "N/A" or empty
                }
            } else {
                book.setPrice(null); // Set to null if price is null
            }

            switch (unified.getSource()) {
                case "Local":
                    sortedLocal.add(book);
                    break;
                case "Google":
                    GoogleBookDto googleDto = new GoogleBookDto();
                    googleDto.setTitle(unified.getTitle());
                    googleDto.setAuthor(unified.getAuthor());
                    googleDto.setImageUrl(unified.getImageUrl());
                    sortedGoogle.add(googleDto);
                    break;
                case "OpenLibrary":
                    OpenLibraryBookDto openDto = new OpenLibraryBookDto();
                    openDto.setTitle(unified.getTitle());
                    openDto.setAuthor(unified.getAuthor());
                    openDto.setImageUrl(unified.getImageUrl());
                    sortedOpen.add(openDto);
                    break;
                case "Amazon":
                    AmazonBookDto amazonDto = new AmazonBookDto();
                    amazonDto.setTitle(unified.getTitle());
                    amazonDto.setAuthor(unified.getAuthor());
                    amazonDto.setProductUrl(unified.getImageUrl()); // Note: Using imageUrl as productUrl
                    sortedAmazon.add(amazonDto);
                    break;
                default:
                    // Ignore unknown sources
                    break;
            }
        }

        // E) Return the response in the expected format
        return new BookSearchResponse(sortedLocal, sortedGoogle, sortedOpen, sortedAmazon);
    }

    @PostMapping("/book/update")
    public ResponseEntity<String> updateBook(@RequestBody BookUpdateRequest request, @RequestHeader("Authorization") String token) {
        try {
            if (token == null || !token.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token");
            }

            Book updatedBook = localBookService.updateBookInformation(
                    request.getTitle(),
                    request.getAuthor(),
                    request.getPrice()
            );
            return ResponseEntity.ok("Book updated/added successfully with ID: " + updatedBook.getId());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error updating/adding book: " + e.getMessage());
        }
    }

    @GetMapping("/most-searched")
    public ResponseEntity<List<String>> getMostSearchedBooks() {
        try {
            List<String> mostSearched = localBookService.getMostSearchedBooks(5);
            return ResponseEntity.ok(mostSearched);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    static int computeScore(String query, String title, String author) {
        int score = 0;
        if (query == null || query.trim().isEmpty()) return score;

        String q = query.toLowerCase().trim().replaceAll("[^a-z0-9 ]", "");
        String[] queryWords = q.split("\\s+");

        if (title != null) {
            String t = title.toLowerCase().trim().replaceAll("[^a-z0-9 ]", "");

            if (t.equals(q)) {
                score += 100; // exact title match
            } else if (t.startsWith(q + " ") || t.equals(q)) {
                score += 80; // title starts with query
            } else if (t.contains(q)) {
                score += 60; // title contains query
            }

            // Partial word matching boosts
            for (String word : queryWords) {
                if (t.contains(word)) score += 10;
                if (t.startsWith(word)) score += 5;
            }
        }

        if (author != null) {
            String a = author.toLowerCase().trim().replaceAll("[^a-z0-9 ]", "");
            if (a.contains(q)) score += 15;
            for (String word : queryWords) {
                if (a.contains(word)) score += 5;
            }
        }

        return score;
    }

}
