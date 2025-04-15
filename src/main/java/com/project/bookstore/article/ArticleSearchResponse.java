package com.project.bookstore.article;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ArticleSearchResponse {
    private List<Article> localResults;
    private List<ArticleDto> semanticDto;

    public ArticleSearchResponse(List<Article> localResults, List<ArticleDto> semanticDto) {
        this.localResults = localResults;
        this.semanticDto = semanticDto;
    }
}
