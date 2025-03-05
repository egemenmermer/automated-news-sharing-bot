package com.egemen.TweetBotTelegram.repository;

import com.egemen.TweetBotTelegram.entity.FetchLogs;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FetchLogsRepository extends JpaRepository<FetchLogs, Long> {
    void deleteByBotId(Long botId);
}
