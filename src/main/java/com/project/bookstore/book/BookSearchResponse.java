package com.project.bookstore.book;

import lombok.Getter;
import lombok.Setter;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Getter
@Setter
public class BookSearchResponse {
    private boolean single;
    private Object singleResult;

    private List<Book> localResults;
    private List<GoogleBookDto> googleBookDto;
    private List<OpenLibraryBookDto> openLibraryResults;
    private List<AmazonBookDto> amazonBookResults;

    public BookSearchResponse(Book onlyLocalHit) {
        this.single = true;
        this.singleResult = onlyLocalHit;
    }

    public BookSearchResponse(List<Book> localResults,
                              List<GoogleBookDto> googleBookDto,
                              List<OpenLibraryBookDto> openLibraryResults,
                              List<AmazonBookDto> amazonBookResults) {
        this.single = false;
        this.localResults = localResults;
        this.googleBookDto = googleBookDto;
        this.openLibraryResults = openLibraryResults;
        this.amazonBookResults = amazonBookResults;
    }
}
