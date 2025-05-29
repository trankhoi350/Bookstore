package com.project.bookstore.book;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SearchHistoryRepository extends JpaRepository<SearchHistory, Long> {
    // Get the most-searched queries, grouped by query, ordered by count
    @Query("SELECT sh.query, COUNT(sh) as searchCount FROM SearchHistory sh GROUP BY sh.query ORDER BY searchCount DESC")
    List<Object[]> findMostSearchedQueries();
}
