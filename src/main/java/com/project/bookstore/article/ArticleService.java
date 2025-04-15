package com.project.bookstore.article;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ArticleService {
    private final ArticleRepository articleRepository;

    public ArticleService(ArticleRepository articleRepository) {
        this.articleRepository = articleRepository;
    }

    public List<Article> getAllArticles() {
        return articleRepository.findAll();
    }

    public List<Article> fullTextSearch(String userInput) {
        String tsQuery = userInput.trim().replaceAll("\\s+", "&");
        return articleRepository.searchByFullText(tsQuery);
    }
}
