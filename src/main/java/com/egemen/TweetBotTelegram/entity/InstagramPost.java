package com.egemen.TweetBotTelegram.entity;

import com.egemen.TweetBotTelegram.enums.PostStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "instagram_posts")
public class InstagramPost {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "news_id")
    private News news;

    @Column(columnDefinition = "TEXT")
    private String imageUrl;

    @Column(columnDefinition = "TEXT")
    private String caption;

    @Column(columnDefinition = "TEXT")
    private String instagramPostId;

    @Enumerated(EnumType.STRING)
    private PostStatus status;

    private Integer retryCount = 0;
    private LocalDateTime postedAt;

    @ManyToOne
    @JoinColumn(name = "bot_id")
    private Bot bot;

    @Column
    private LocalDateTime createdAt;

    public void incrementRetryCount() {
        this.retryCount = (this.retryCount == null ? 0 : this.retryCount) + 1;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public News getNews() {
        return news;
    }

    public void setNews(News news) {
        this.news = news;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getCaption() {
        return caption;
    }

    public void setCaption(String caption) {
        this.caption = caption;
    }

    public String getInstagramPostId() {
        return instagramPostId;
    }

    public void setInstagramPostId(String instagramPostId) {
        this.instagramPostId = instagramPostId;
    }

    public PostStatus getStatus() {
        return status;
    }

    public void setStatus(PostStatus status) {
        this.status = status;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }

    public LocalDateTime getPostedAt() {
        return postedAt;
    }

    public void setPostedAt(LocalDateTime postedAt) {
        this.postedAt = postedAt;
    }

    public Bot getBot() {
        return bot;
    }

    public void setBot(Bot bot) {
        this.bot = bot;
    }
}