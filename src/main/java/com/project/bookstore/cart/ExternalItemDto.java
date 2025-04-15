package com.project.bookstore.cart;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ExternalItemDto {
    private String externalId;
    private String title;
    private String authors;
    private String thumbnailUrl;

    public ExternalItemDto() {
    }

    public ExternalItemDto(String externalId, String title, String authors, String thumbnailUrl) {
        this.externalId = externalId;
        this.title = title;
        this.authors = authors;
        this.thumbnailUrl = thumbnailUrl;
    }
}
