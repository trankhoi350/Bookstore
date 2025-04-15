package com.project.bookstore.book;

import java.math.BigDecimal;

public record UnifiedBookDto(
        String source,
        String id,
        String title,
        String author,
        String isbn,
        String price,
        Integer publicationYear,
        Integer pageCount,
        String genre,
        String imageUrl
) {}

