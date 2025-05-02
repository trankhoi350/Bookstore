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
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String author;
    private String isbn;
    private BigDecimal price;

    @Column(length = 2000)
    private String description;
    private LocalDate publishDate;
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
