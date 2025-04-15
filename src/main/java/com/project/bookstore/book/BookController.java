package com.project.bookstore.book;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    public ResponseEntity<BookSearchResponse> search(@RequestParam String query) {
        //Local DB
        var localHits = localBookService.fullTextSearch(query);
        var googleHits = googleBookService.searchGoogleBooks(query);
        var openLibraryHits = openLibraryService.searchOpenLibraryBook(query);
        var amazonHits = amazonService.searchAmazonBooks(query);

        int total = localHits.size() + googleHits.size() + openLibraryHits.size() + amazonHits.size();

        if (total == 1) {
            UnifiedBookDto single;
            if (!localHits.isEmpty()) {
                var books = localHits.get(0);
                single = new UnifiedBookDto(
                        "local",
                        books.getId().toString(),
                        books.getTitle(),
                        books.getAuthor(),
                        books.getIsbn(),
                        books.getPrice().toString(),
                        books.getPublishDate() != null ? books.getPublishDate().getYear() : null,
                        null, null, null
                );
            }
            else if (!googleHits.isEmpty()) {
                var goolgeBooks = googleHits.get(0);
                single = new UnifiedBookDto(
                        "google",
                        goolgeBooks.getId(),
                        goolgeBooks.getTitle(),
                        goolgeBooks.getAuthor(),
                        goolgeBooks.getIsbn(),
                        goolgeBooks.getPrice().toString(),
                        goolgeBooks.getPublicationYear(),
                        goolgeBooks.getPageCount(),
                        goolgeBooks.getGenre(),
                        goolgeBooks.getImageUrl()
                );
            }
            else if (!openLibraryHits.isEmpty()) {
                var openLibraryBooks = openLibraryHits.get(0);
                single = new UnifiedBookDto(
                        "openlibrary",
                        openLibraryBooks.getKey(),
                        openLibraryBooks.getTitle(),
                        openLibraryBooks.getAuthor(),
                        openLibraryBooks.getIsbn(),
                        openLibraryBooks.getPrice().toString(),
                        openLibraryBooks.getPublicationYear(),
                        openLibraryBooks.getPageCount(),
                        openLibraryBooks.getGenre(),
                        openLibraryBooks.getImageUrl()
                );
            }
            else {
                var amazonBooks = amazonHits.get(0);
                single = new UnifiedBookDto(
                        "amazon", null,
                        amazonBooks.getTitle(),
                        amazonBooks.getAuthor(),
                        null,
                        amazonBooks.getPrice(),
                        null,
                        null,
                        null,
                        amazonBooks.getProductUrl()
                );
            }
            return ResponseEntity.ok(new BookSearchResponse(single));
        }
        return ResponseEntity.ok(new BookSearchResponse(localHits, googleHits, openLibraryHits, amazonHits));
    }
}
