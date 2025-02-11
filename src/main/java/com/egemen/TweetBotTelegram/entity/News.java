package com.egemen.TweetBotTelegram.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "news")
public class News {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String title;
    private String content;
    private String description;
    private String imageUrl;
    private String generatedImagePath;
    private LocalDateTime publishedAt;
    private boolean processed;
    private boolean posted;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    // Getters
    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getDescription() { return description; }
    public String getImageUrl() { return imageUrl; }
    public String getGeneratedImagePath() { return generatedImagePath; }
    public LocalDateTime getPublishedAt() { return publishedAt; }
    public boolean isProcessed() { return processed; }
    public boolean isPosted() { return posted; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    // Setters
    public void setId(Long id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setContent(String content) { this.content = content; }
    public void setDescription(String description) { this.description = description; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public void setGeneratedImagePath(String generatedImagePath) { this.generatedImagePath = generatedImagePath; }
    public void setPublishedAt(LocalDateTime publishedAt) { this.publishedAt = publishedAt; }
    public void setProcessed(boolean processed) { this.processed = processed; }
    public void setPosted(boolean posted) { this.posted = posted; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
