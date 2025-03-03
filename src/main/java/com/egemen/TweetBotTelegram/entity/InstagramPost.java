package com.egemen.TweetBotTelegram.entity;

import com.egemen.TweetBotTelegram.enums.PostStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Entity
@Table(name = "instagram_posts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InstagramPost {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "bot_id", nullable = false)
    private Bot bot;

    @ManyToOne
    @JoinColumn(name = "news_id")
    private News news;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String caption;

    @Column(columnDefinition = "TEXT")
    private String imageUrl;

    @Column(columnDefinition = "TEXT")
    private String imagePrompt;

    private String instagramPostId;

    @Enumerated(EnumType.STRING)
    private PostStatus postStatus;

    private Timestamp createdAt;

    private Timestamp postedAt;

    private Integer retryCount = 0;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;
}
