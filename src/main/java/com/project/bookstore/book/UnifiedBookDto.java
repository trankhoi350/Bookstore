package com.project.bookstore.book;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UnifiedBookDto {
    private String title;
    private String author;
    private String imageUrl;
    private String source;
    private String price;

    public UnifiedBookDto(String title, String author, String imageUrl, String source, String price) {
        this.title = title;
        this.author = author;
        this.imageUrl = imageUrl;
        this.source = source;
        this.price = price;
    }
}
