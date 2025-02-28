package com.egemen.TweetBotTelegram.repository;

import com.egemen.TweetBotTelegram.entity.SummarizedNews;
import com.egemen.TweetBotTelegram.enums.SummarizedStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SummarizedNewsRepository extends JpaRepository<SummarizedNews, Long> {
    @Query("SELECT bc FROM SummarizedNews bc WHERE bc.status = :summarizeStatus")
    List<SummarizedNews> findByStatus(@Param("summarizeStatus") SummarizedStatus summarizeStatus);
}
