package com.project.bookstore.book;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

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
    public BookSearchResponse search(@RequestParam String query) {
        // A) first, search your own database
        List<Book> local = localBookService.fullTextSearch(query);

        // B) see if *any* local hit is an exact title match
        Optional<Book> exact = local.stream()
                .filter(b -> b.getTitle().equalsIgnoreCase(query))
                .findFirst();

        if (exact.isPresent()) {
            // as soon as we find *one* exact match, return "single"
            return new BookSearchResponse(exact.get());
        }

        // C) no exact local match → now hit the external APIs
        List<GoogleBookDto>   google = googleBookService.searchGoogleBooks(query);
        List<OpenLibraryBookDto> open  = openLibraryService.searchOpenLibraryBook(query);
        List<AmazonBookDto>      amazon= amazonService.searchAmazonBooks(query);

        // D) wrap them all up in the normal multi‐list constructor
        return new BookSearchResponse(local, google, open, amazon);
    }
}
