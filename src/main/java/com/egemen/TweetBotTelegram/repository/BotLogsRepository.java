package com.egemen.TweetBotTelegram.repository;

import com.egemen.TweetBotTelegram.entity.BotLogs;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BotLogsRepository extends JpaRepository<BotLogs, Long> {
    void deleteByBotId(Long botId);
}
