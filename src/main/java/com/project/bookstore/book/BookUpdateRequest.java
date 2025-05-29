package com.project.bookstore.book;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class BookUpdateRequest {
    private String title;
    private String author;
    private BigDecimal price;
}
