package com.project.bookstore.article;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "articles")
public class Article {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;
    private String abstractText;
    private String author;
    private String journal;
    private int publicationYear;

    public Article() {
    }

    public Article(Long id, String title, String abstractText, String author, String journal, int publicationYear) {
        this.id = id;
        this.title = title;
        this.abstractText = abstractText;
        this.author = author;
        this.journal = journal;
        this.publicationYear = publicationYear;
    }

    @Override
    public String toString() {
        return "Article{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", abstractText='" + abstractText + '\'' +
                ", author='" + author + '\'' +
                ", journal='" + journal + '\'' +
                ", publicationYear=" + publicationYear +
                '}';
    }
}
