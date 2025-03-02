package com.egemen.TweetBotTelegram.entity;

import com.egemen.TweetBotTelegram.enums.NewsStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

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

    @Column(name = "title", columnDefinition = "TEXT")
    private String title;

    @Lob
    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Lob
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;

    @Column(name = "generated_image_path", columnDefinition = "TEXT")
    private String generatedImagePath;

    @Column(name = "source")
    private String source;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private NewsStatus status = NewsStatus.PENDING;

    @Column(name = "published_at", nullable = false)
    private Timestamp publishedAt;

    @Column(name = "created_at")
    private Timestamp createdAt;

    public News(Bot bot, String title, String content, String source, Timestamp publishedAt, NewsStatus status) {
        this.bot = bot;
        this.title = title;
        this.content = content;
        this.source = source;
        this.publishedAt = publishedAt;
        this.status = status;
    }
}
