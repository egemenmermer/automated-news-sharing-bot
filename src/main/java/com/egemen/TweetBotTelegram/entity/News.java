package com.egemen.TweetBotTelegram.entity;

import com.egemen.TweetBotTelegram.enums.NewsStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.sql.Timestamp;

@Entity
@Table(name = "news")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class News {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "bot_id", nullable = false)
    private Bot bot;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    private String source;

    private String url;

    private String imageUrl;

    private String category;

    private Timestamp publishedAt;

    @Enumerated(EnumType.STRING)
    private NewsStatus status;

    // Add any additional fields or methods as needed
}
