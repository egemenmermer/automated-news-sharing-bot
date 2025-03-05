package com.egemen.TweetBotTelegram.repository;

import com.egemen.TweetBotTelegram.entity.Bot;
import com.egemen.TweetBotTelegram.entity.PostLogs;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PostLogsRepository extends JpaRepository<PostLogs, Long> {
    @Query("SELECT COUNT(p) FROM PostLogs p WHERE p.bot = :bot")
    long countByBot(@Param("bot") Bot bot);

    void deleteByBotId(Long botId);
}
