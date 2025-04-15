package com.project.bookstore.article;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ArticleRepository extends JpaRepository<Article, Long> {
    Optional<Article> findArticlesById(Long id);

    @Query(value = """
        SELECT *, ts_rank(to_tsvector('english', title || ' ' || abstract_text), to_tsquery('english', :query)) AS rank
        FROM articles WHERE to_tsvector('english', title || ' ' || abstract_text) @@ to_tsquery('english', :query)
        ORDER BY rank DESC
    """, nativeQuery = true)
    List<Article> searchByFullText(@Param("query") String query);
}
