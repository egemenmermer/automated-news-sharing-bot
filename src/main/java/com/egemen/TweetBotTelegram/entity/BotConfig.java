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
    @Column(name = "id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "bot_id")
    private Bot bot;

    @Column(name = "config_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private ConfigType configType;

    @Column(name = "config_value", nullable = false)
    private String configValue;

    @Column(name = "created_at")
    private Timestamp createdAt;

}


