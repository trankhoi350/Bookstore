package com.project.bookstore.cart;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class CartItemRequest {
    //Internal books and articles
    private Long bookId;
    private Long articleId;


    private int quantity;

    //External books and articles
    private String externalId;
    @JsonProperty("title")
    private String externalTitle;
    @JsonProperty("author")
    private String externalAuthor;
    private String isbn;
    private String description;
    private String publicationYear;
    private BigDecimal price;
    private String genre;

    //Define the type
    private ItemSource itemSource;
    private ItemType itemType;
}
