package com.project.bookstore.book;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {
    Optional<Book> findBooksById(Long id);
    List<Book> findBooksByTitle(String title);

    @Query(value = """
        SELECT *, ts_rank(to_tsvector('english', title || ' ' || description), to_tsquery('english', :query)) AS rank
        FROM books WHERE to_tsvector('english', title || ' ' || description) @@ to_tsquery('english', :query)
        ORDER BY rank DESC
    """, nativeQuery = true)
    List<Book> searchByFullText(@Param("query") String query);

    @Query("SELECT b FROM Book b WHERE LOWER(b.title) = LOWER(:title)")
    Book findByTitleIgnoreCase(@Param("title") String title);
}
