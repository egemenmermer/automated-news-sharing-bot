package com.egemen.TweetBotTelegram.entity;


import com.egemen.TweetBotTelegram.enums.ConfigType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "bot_configurations")
public class BotConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long Id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "bot_id", nullable = false)
    private Bot botId;

    @Column(nullable = false, name = "config_type")
    private String configType;

    @Column(nullable = false, name = "config_value")
    private String configValue;

    @Column(nullable = false, name = "created_at")
    private Timestamp createdAt;

}


