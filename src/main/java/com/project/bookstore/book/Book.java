package com.project.bookstore.book;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "books")
@Getter
@Setter
public class Book {
    @Getter
    @Setter
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Getter
    @Setter
    private String title;
    @Getter
    @Setter
    private String author;
    @Getter
    private String isbn;
    @Getter
    @Setter
    private BigDecimal price;

    @Column(length = 2000)
    private String description;
    @Getter
    @Setter
    private LocalDate publishDate;

    @Setter
    @Getter
    private String imageUrl;

    public Book() {
    }

    public Book(Long id) {
        this.id = id;
    }

    public Book(Long id, String title, String author, String isbn, BigDecimal price, String description, LocalDate publishDate, String imageUrl) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.isbn = isbn;
        this.price = price;
        this.description = description;
        this.publishDate = publishDate;
        this.imageUrl = imageUrl;
    }

    @Override
    public String toString() {
        return "Book{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", author='" + author + '\'' +
                ", isbn='" + isbn + '\'' +
                ", price=" + price +
                ", description='" + description + '\'' +
                ", publishDate=" + publishDate +
                '}';
    }
}
