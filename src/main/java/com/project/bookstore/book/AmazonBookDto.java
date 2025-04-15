package com.project.bookstore.book;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Service;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AmazonBookDto {
    private String title;
    private String author;
    private String price;
    private String isbn;
    private String productUrl;


    public AmazonBookDto(String title, String author, String price, String productUrl) {
        this.title = title;
        this.author = author;
        this.price = price;
        this.productUrl = productUrl;
    }
}
