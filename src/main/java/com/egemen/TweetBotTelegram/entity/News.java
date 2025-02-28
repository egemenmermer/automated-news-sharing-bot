package com.egemen.TweetBotTelegram.entity;

import com.egemen.TweetBotTelegram.enums.NewsStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "news")
public class News {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "bot_id")
    private Bot bot;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "content", nullable = false)
    private String content;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private NewsStatus status;

    @Column(name = "published_at")
    private Timestamp publishedAt;

    @Column(name = "is_processed")
    private boolean isProcessed;



    public News(Bot bot, String title, String content, Timestamp publishedAt, NewsStatus status) {
        this.bot = bot;
        this.title = title;
        this.content = content;
        this.publishedAt = publishedAt;
        this.status = status;
    }
}
