package com.egemen.TweetBotTelegram.repository;

import com.egemen.TweetBotTelegram.entity.Bot;
import com.egemen.TweetBotTelegram.entity.BotConfig;
import com.egemen.TweetBotTelegram.enums.ConfigType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BotConfigRepository extends JpaRepository<BotConfig, Long> {

    @Query("SELECT bc.configValue FROM BotConfig bc WHERE bc.bot = :bot AND bc.configType = :configType")
    Optional<String> findConfigValueByBotIdAndConfigType(@Param("bot") Bot bot, @Param("configType") ConfigType configType);

    @Query("SELECT bc FROM BotConfig bc WHERE bc.bot = :bot")
    List<BotConfig> findBotConfigurationsByBot(@Param("bot") Bot bot);
    
    @Query("SELECT bc FROM BotConfig bc WHERE bc.bot = :bot AND bc.configType = :configType")
    Optional<BotConfig> findByBotAndConfigType(@Param("bot") Bot bot, @Param("configType") ConfigType configType);

}
