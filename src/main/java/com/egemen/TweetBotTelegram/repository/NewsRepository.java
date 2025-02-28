package com.egemen.TweetBotTelegram.repository;

import com.egemen.TweetBotTelegram.entity.News;
import com.egemen.TweetBotTelegram.enums.NewsStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NewsRepository extends JpaRepository<News, Long> {
    List<News> findByIsProcessed(boolean isProcessed);

    @Query("SELECT bc FROM News bc WHERE bc.status = :newsStatus")
    List<News> findByStatus(@Param("newsStatus") NewsStatus newsStatus);
}
