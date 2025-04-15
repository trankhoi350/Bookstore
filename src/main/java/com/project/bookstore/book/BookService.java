package com.project.bookstore.book;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BookService {
    private final BookRepository bookRepository;

    @Autowired
    public BookService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    public List<Book> getBooks() {
        return bookRepository.findAll();
    }

    public List<Book> fullTextSearch(String userInput) {
        String tsQuery = userInput.trim().replaceAll("\\s+", "&");
        return bookRepository.searchByFullText(tsQuery);
    }
}
