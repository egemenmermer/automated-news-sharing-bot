package com.egemen.TweetBotTelegram.entity;

import com.egemen.TweetBotTelegram.enums.LogType;
import jakarta.persistence.*;

import java.sql.Timestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "bot_logs")
public class BotLogs {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "bot_id")
    private Bot bot;

    @Column(name = "log_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private LogType logType;

    @Column(name = "log_message", nullable = false)
    private String logMessage;

    @Column(name = "created_at")
    private Timestamp createdAt;
} 