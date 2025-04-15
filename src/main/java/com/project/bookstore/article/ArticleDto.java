package com.project.bookstore.article;

import lombok.Getter;
import lombok.Setter;
import org.json.JSONArray;

@Getter
@Setter
public class ArticleDto {
    private String id;
    private String title;
    private String author;

    public ArticleDto(String id, String title, String author) {
        this.id = id;
        this.title = title;
        this.author = author;
    }
}
