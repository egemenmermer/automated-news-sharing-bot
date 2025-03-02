package com.egemen.TweetBotTelegram.repository;

import com.egemen.TweetBotTelegram.entity.Bot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BotRepository extends JpaRepository<Bot, Long> {
    Optional<Bot> findById(Long botId);
    Optional<Bot> findByName(String name);
    // Temporarily commenting out method that references the missing isActive field
    // List<Bot> findByIsActiveTrue();
    
    // Temporary replacement method to get all bots while isActive field is unavailable
    default List<Bot> findAllActiveBots() {
        return findAll();
    }
}
