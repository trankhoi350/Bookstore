package com.project.bookstore.cart;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.project.bookstore.article.Article;
import com.project.bookstore.book.Book;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "cart_items")
public class CartItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String externalId;
    private String externalAuthor;
    private String externalTitle;

    @ManyToOne
    @JoinColumn(name = "cart_id", referencedColumnName = "id")
    @JsonBackReference
    private Cart cart;

    @ManyToOne
    @JoinColumn(name = "book_id", referencedColumnName = "id")
    private Book book;

    @ManyToOne
    @JoinColumn(name = "article_id", referencedColumnName = "id")
    private Article article;

    private int quantity;
    private String genre;
    private String publicationYear;
    private String isbn;

    @Enumerated(EnumType.STRING)
    private ItemSource itemSource;

    @Enumerated(EnumType.STRING)
    private ItemType itemType;
}
