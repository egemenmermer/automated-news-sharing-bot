package com.egemen.TweetBotTelegram.repository;

import com.egemen.TweetBotTelegram.entity.Bot;
import com.egemen.TweetBotTelegram.entity.News;
import com.egemen.TweetBotTelegram.enums.NewsStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NewsRepository extends JpaRepository<News, Long> {
    List<News> findByStatus(NewsStatus status);

    @Query("SELECT n FROM News n WHERE n.status = :newsStatus ORDER BY n.publishedAt DESC")
    List<News> findLatestByStatus(@Param("newsStatus") NewsStatus newsStatus);

    @Query(value = "SELECT n FROM News n WHERE n.status = :status ORDER BY n.publishedAt DESC")
    List<News> findByStatus(@Param("status") NewsStatus status, Pageable pageable);
    
    List<News> findByStatusOrderByPublishedAtDesc(NewsStatus status);

    List<News> findByBot(Bot bot);
    
    List<News> findByBotAndStatus(Bot bot, NewsStatus status);
    
    @Query(value = "SELECT * FROM news WHERE bot_id = ?1 AND status = ?2 ORDER BY published_at DESC LIMIT ?3", nativeQuery = true)
    List<News> findByBotAndStatusOrderByPublishedAtDesc(Bot bot, NewsStatus status, int limit);
    
    boolean existsByTitleAndBot(String title, Bot bot);

    List<News> findByBotAndStatusOrderByPublishedAtDesc(Bot bot, NewsStatus status, Pageable pageable);
}
