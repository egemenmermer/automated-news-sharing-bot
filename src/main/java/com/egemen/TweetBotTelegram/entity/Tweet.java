package com.egemen.TweetBotTelegram.entity;

import com.egemen.TweetBotTelegram.enums.TweetStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Entity
@Table(name = "tweets")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Tweet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "bot_id")
    private Bot bot;

    @ManyToOne
    @JoinColumn(name = "news_id")
    private News news;

    @Column(name = "content", nullable = false)
    private String content;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private TweetStatus status;

    @Column(name = "retry_count")
    private int retryCount;

    @Column(name = "scheduled_at")
    private Timestamp scheduledAt;

    @Column(name = "created_at")
    private Timestamp createdAt;

    public Tweet(Bot bot, News news, String tweetContent, TweetStatus tweetStatus) {
    }
}
