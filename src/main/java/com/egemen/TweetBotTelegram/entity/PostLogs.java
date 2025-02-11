package com.egemen.TweetBotTelegram.entity;

import com.egemen.TweetBotTelegram.enums.PostStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.time.LocalDateTime;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "post_logs")
public class PostLogs {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "bot_id", nullable = false)
    private Bot botId;

    @Column(name = "scheduled_at")
    private Timestamp scheduledAt;

    @Column(name = "posted_at")
    private Timestamp postedAt;

    @Column(nullable = false, name = "status")
    @Enumerated(EnumType.STRING)
    private PostStatus status;

    @Column(name = "log_message")
    private String logMessage;

    private LocalDateTime createdAt;
    private String message;
}
