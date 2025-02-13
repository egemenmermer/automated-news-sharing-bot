package com.egemen.TweetBotTelegram.repository;

import com.egemen.TweetBotTelegram.entity.News;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NewsRepository extends JpaRepository<News, Long> {
    @Query(value = "SELECT * FROM news ORDER BY published_at DESC LIMIT ?1", nativeQuery = true)
    List<News> findTopByOrderByPublishedAtDesc(int limit);
    List<News> findByProcessedFalse();
    List<News> findByProcessedFalseAndPostedFalseOrderByCreatedAtDesc();
    List<News> findByCreatedAtAfter(LocalDateTime date);
    boolean existsByTitleAndContent(String title, String content);
}
