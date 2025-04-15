package com.project.bookstore.article;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/articles")
public class ArticleController {
    private final ArticleService articleService;
    private final SemanticScholarService semanticScholarService;

    @Autowired
    public ArticleController(ArticleService articleService, SemanticScholarService semanticScholarService) {
        this.articleService = articleService;
        this.semanticScholarService = semanticScholarService;
    }

    @GetMapping("/search")
    public ArticleSearchResponse search(@RequestParam String query) {
        // Query local DB
        List<Article> localResults = articleService.fullTextSearch(query);

        // Query Semantic Scholar
        List<ArticleDto> semanticResults = semanticScholarService.searchSemanticScholar(query);

        return new ArticleSearchResponse(localResults, semanticResults);
    }


}
