package com.egemen.TweetBotTelegram.repository;

import com.egemen.TweetBotTelegram.entity.News;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NewsRepository extends JpaRepository<News, Long> {
    List<News> findByProcessedFalse();
    List<News> findByProcessedFalseAndPostedFalseOrderByCreatedAtDesc();
    List<News> findByCreatedAtAfter(LocalDateTime date);
    boolean existsByTitleAndDescription(String title, String description);
}
