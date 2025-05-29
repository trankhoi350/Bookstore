package com.project.bookstore.book;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Service;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AmazonBookDto {
    private String id;
    private String title;
    private String author;
    private String price;
    private String isbn;
    private String productUrl;


    public AmazonBookDto(String id, String title, String author, String price, String productUrl) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.price = price;
        this.productUrl = productUrl;
    }

    public AmazonBookDto() {

    }
}
