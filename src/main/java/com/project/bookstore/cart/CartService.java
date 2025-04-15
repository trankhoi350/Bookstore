package com.project.bookstore.cart;

import com.project.bookstore.article.Article;
import com.project.bookstore.article.ArticleRepository;
import com.project.bookstore.book.Book;
import com.project.bookstore.book.BookRepository;
import com.project.bookstore.user.User;
import com.project.bookstore.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CartService {
    private final CartRepository cartRepository;
    private final UserRepository userRepository;
    private final ArticleRepository articleRepository;
    private final BookRepository bookRepository;

    @Autowired
    public CartService(CartRepository cartRepository, UserRepository userRepository, ArticleRepository articleRepository, BookRepository bookRepository) {
        this.cartRepository = cartRepository;
        this.userRepository = userRepository;
        this.articleRepository = articleRepository;
        this.bookRepository = bookRepository;
    }

    public Cart getUserCart(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return cartRepository.findByUser(user).orElseGet(() -> createCart(user));

    }

    private Cart createCart(User user) {
        Cart cart = new Cart();
        cart.setUser(user);
        return cartRepository.save(cart);
    }

    public Cart addItemToCart(String email, CartItemRequest request) {
        Cart cart = getUserCart(email);
        CartItem item = new CartItem();
        item.setCart(cart);
        item.setQuantity(request.getQuantity());
        item.setItemSource(request.getItemSource());
        item.setItemType(request.getItemType());
        item.setExternalTitle(request.getExternalTitle());
        item.setExternalAuthor(request.getExternalAuthor());
        item.setGenre(request.getGenre());
        item.setIsbn(request.getIsbn());
        item.setPublicationYear(request.getPublicationYear());

        if (request.getItemSource() == ItemSource.INTERNAL) {
            if (request.getItemType() == ItemType.BOOK) {
                Book book = bookRepository.findBooksById(request.getBookId()).orElseThrow(() -> new RuntimeException("Book not found"));
                item.setBook(book);
            }
            else if (request.getItemType() == ItemType.ARTICLE) {
                Article article = articleRepository.findArticlesById(request.getArticleId()).orElseThrow(() -> new RuntimeException("Article not found"));
                item.setArticle(article);
            }
        }
        else if (request.getItemSource() == ItemSource.EXTERNAL) {
            item.setItemSource(ItemSource.EXTERNAL);
            item.setExternalId(request.getExternalId());
        }
        cart.getItems().add(item);
        return cartRepository.save(cart);
    }
}
