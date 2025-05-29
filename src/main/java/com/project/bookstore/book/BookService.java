package com.project.bookstore.book;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.project.bookstore.book.BookController.computeScore;

@Service
public class BookService {
    private final BookRepository bookRepository;
    private final GoogleBookService googleBookService;
    private final OpenLibraryService openLibraryService;
    private final AmazonService amazonService;
    private final SearchHistoryRepository searchHistoryRepository;

    @Autowired
    public BookService(BookRepository bookRepository, GoogleBookService googleBookService, OpenLibraryService openLibraryService, AmazonService amazonService, SearchHistoryRepository searchHistoryRepository) {
        this.bookRepository = bookRepository;
        this.googleBookService = googleBookService;
        this.openLibraryService = openLibraryService;
        this.amazonService = amazonService;
        this.searchHistoryRepository = searchHistoryRepository;
    }

    public List<Book> getBooks() {
        return bookRepository.findAll();
    }

    public List<Book> fullTextSearch(String userInput) {
        // Normalize the input query: trim spaces, remove special characters, and convert to lowercase
        String normalizedQuery = userInput.trim().toLowerCase().replaceAll("[^a-z0-9\\s]", "");

        // Check for exact match in the database
        Book exactMatch = bookRepository.findByTitleIgnoreCase(userInput.trim());
        if (exactMatch != null) {
            // Double-check the match with normalized comparison
            String storedTitle = exactMatch.getTitle() != null ? exactMatch.getTitle().trim().toLowerCase().replaceAll("[^a-z0-9\\s]", "") : "";
            if (storedTitle.equals(normalizedQuery)) {
                return Collections.singletonList(exactMatch); // Return only the exact match
            }
        }

        // If no exact match, perform full-text search
        String tsQuery = userInput.trim().replaceAll("\\s+", "&");
        return bookRepository.searchByFullText(tsQuery);
    }

    public List<UnifiedBookDto> smartSearch(String query) {
        String normalizedQuery = normalize(query);

        // 1. Local DB search
        List<Book> localBooks = fullTextSearch(query);

        // 2. External API searches
        List<GoogleBookDto> googleBookDtos = googleBookService.searchGoogleBooks(query);
        List<OpenLibraryBookDto> openLibraryBookDtos = openLibraryService.searchOpenLibraryBook(query);
        List<AmazonBookDto> amazonBookDtos = amazonService.searchAmazonBooks(query);

        // 3. Convert external DTOs to Book
        List<Book> googleBooks = googleBookDtos.stream()
                .map(this::convertGoogleDtoToBook)
                .toList();

        List<Book> openLibraryBooks = openLibraryBookDtos.stream()
                .map(this::convertOpenLibraryDtoToBook)
                .toList();

        List<Book> amazonBooks = amazonBookDtos.stream()
                .map(this::convertAmazonDtoToBook)
                .toList();

        // 4. Combine all books
        List<Book> allBooks = new ArrayList<>();
        allBooks.addAll(localBooks);
        allBooks.addAll(googleBooks);
        allBooks.addAll(openLibraryBooks);
        allBooks.addAll(amazonBooks);

        // 5. Score and categorize books
        List<ScoredBook> startsWithBooks = new ArrayList<>();
        List<ScoredBook> containsBooks = new ArrayList<>();

        for (Book book : allBooks) {
            String title = book.getTitle() != null ? book.getTitle() : "";
            String normalizedTitle = normalize(title);
            String author = book.getAuthor() != null ? book.getAuthor() : "";

            // Align normalization for scoring
            int score = computeScore(normalizedQuery, normalizedTitle, normalize(author));

            // Check if the title starts with the query or any word in the title matches the query
            if (normalizedTitle.startsWith(normalizedQuery + " ") || normalizedTitle.equals(normalizedQuery)) {
                startsWithBooks.add(new ScoredBook(book, score));
            } else if (normalizedTitle.contains(normalizedQuery) || score > 0) {
                containsBooks.add(new ScoredBook(book, score));
            }
        }

        // 6. Sort each group
        // Prioritize startsWithBooks by ensuring exact startsWith gets a boost
        startsWithBooks.sort((a, b) -> {
            String titleA = normalize(((Book) a.original).getTitle());
            String titleB = normalize(((Book) b.original).getTitle());
            // Boost for exact match at the start
            boolean aExactStart = titleA.equals(normalizedQuery) || titleA.startsWith(normalizedQuery + " ");
            boolean bExactStart = titleB.equals(normalizedQuery) || titleB.startsWith(normalizedQuery + " ");
            if (aExactStart && !bExactStart) return -1;
            if (!aExactStart && bExactStart) return 1;
            // Fallback to score comparison
            return Integer.compare(b.score, a.score);
        });

        containsBooks.sort((a, b) -> Integer.compare(b.score, a.score));

        // 7. Combine prioritized lists
        List<ScoredBook> prioritizedBooks = new ArrayList<>();
        prioritizedBooks.addAll(startsWithBooks);
        prioritizedBooks.addAll(containsBooks);

        System.out.println("StartsWith books: " + startsWithBooks.size());
        startsWithBooks.forEach(sb -> System.out.println("  - " + ((Book) sb.original).getTitle() + " (Score: " + sb.score + ")"));
        System.out.println("Contains books: " + containsBooks.size());
        containsBooks.forEach(sb -> System.out.println("  - " + ((Book) sb.original).getTitle() + " (Score: " + sb.score + ")"));

        // 8. Convert to UnifiedBookDto
        return prioritizedBooks.stream()
                .map(sb -> {
                    Book book = (Book) sb.original;

                    String source;
                    if (localBooks.contains(book)) {
                        source = "Local";
                    } else if (googleBooks.contains(book)) {
                        source = "Google";
                    } else if (openLibraryBooks.contains(book)) {
                        source = "OpenLibrary";
                    } else if (amazonBooks.contains(book)) {
                        source = "Amazon";
                    } else {
                        source = "Unknown";
                    }

                    return new UnifiedBookDto(
                            book.getTitle(),
                            book.getAuthor(),
                            book.getImageUrl() != null ? book.getImageUrl() : "/placeholder.jpg",
                            source,
                            book.getPrice() != null ? String.valueOf(book.getPrice()) : "N/A"
                    );
                })
                .collect(Collectors.toList());
    }

    private Book convertGoogleDtoToBook(GoogleBookDto dto) {
        Book book = new Book();
        book.setTitle(dto.getTitle());
        book.setAuthor(dto.getAuthor()); // Adjust field names as needed
        book.setIsbn(dto.getIsbn());
        book.setImageUrl(dto.getImageUrl()); // Ensure this field is mapped
        book.setPrice(dto.getPrice()); // Map price if available
        return book;
    }

    private Book convertOpenLibraryDtoToBook(OpenLibraryBookDto dto) {
        Book book = new Book();
        book.setTitle(dto.getTitle());
        book.setAuthor(dto.getAuthor()); // Adjust field names
        book.setIsbn(dto.getIsbn());
        book.setImageUrl(dto.getImageUrl()); // Ensure this field is mapped
        book.setPrice(dto.getPrice()); // Map price if available
        return book;
    }

    private Book convertAmazonDtoToBook(AmazonBookDto dto) {
        Book book = new Book();
        book.setTitle(dto.getTitle());
        book.setAuthor(dto.getAuthor());
        book.setIsbn(dto.getIsbn());
        book.setImageUrl(dto.getProductUrl());

        //Handle price with $ symbol
        String priceStr = dto.getPrice() != null ? dto.getPrice().trim() : null;
        if (priceStr != null && !priceStr.isEmpty()) {
            // Remove $ symbol and check if the remaining string is numeric
            priceStr = priceStr.replace("$", "").trim();
            if (!priceStr.matches("\\d+(\\.\\d+)?")) {
                // If not a valid number (e.g., "N/A"), set price to null
                System.out.println("Invalid price format for book " + dto.getTitle() + ": " + dto.getPrice());
                book.setPrice(null);
            } else {
                book.setPrice(new BigDecimal(priceStr));
            }
        } else {
            book.setPrice(null);
        }
        return book;
    }

    private String normalize(String input) {
        return input.toLowerCase().trim().replaceAll("[^a-z0-9 ]", "");
    }

    public Book updateBookInformation(String title, String author, BigDecimal price) {
        if (title == null || author == null || price == null) {
            throw new IllegalArgumentException("Title, author, and price are required.");
        }

        List<Book> existingBooks = bookRepository.findBooksByTitle(title);
        Book book;

        if (!existingBooks.isEmpty()) {
            book = existingBooks.get(0); // Pick the first match
            book.setTitle(title);
            book.setAuthor(author);
            book.setPrice(price);
        }
        else {
            book = new Book();
            book.setTitle(title);
            book.setAuthor(author);
            book.setPrice(price);
        }
        return bookRepository.save(book);
    }

    // New methods for search history
    public void logSearch(String query, String userToken) {
        if (query == null || query.trim().isEmpty() || userToken == null || userToken.trim().isEmpty()) {
            return; // Skip invalid entries
        }
        SearchHistory search = new SearchHistory();
        search.setQuery(query.trim());
        search.setUserToken(userToken);
        search.setTimestamp(LocalDateTime.now());
        searchHistoryRepository.save(search);
    }

    public List<String> getMostSearchedBooks(int limit) {
        List<Object[]> results = searchHistoryRepository.findMostSearchedQueries();
        return results.stream()
                .limit(limit)
                .map(result -> (String) result[0]) // Extract the query string
                .collect(Collectors.toList());
    }


}
