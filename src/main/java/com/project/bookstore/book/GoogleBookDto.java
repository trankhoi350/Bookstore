package com.project.bookstore.book;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class GoogleBookDto {
    private String id;
    private String title;
    private String author;
    private String isbn;
    private Integer publicationYear;
    private Integer pageCount;
    private String genre;
    private BigDecimal price;
    private String imageUrl;

    public GoogleBookDto() {}

    public GoogleBookDto(String id, String title, String author, String isbn, Integer publicationYear, Integer pageCount, String genre, BigDecimal price, String imageUrl) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.isbn = isbn;
        this.publicationYear = publicationYear;
        this.pageCount = pageCount;
        this.genre = genre;
        this.price = price;
        this.imageUrl = imageUrl;
    }

}
