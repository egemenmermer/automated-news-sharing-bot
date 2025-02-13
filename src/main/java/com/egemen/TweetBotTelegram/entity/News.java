package com.egemen.TweetBotTelegram.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    @JoinColumn(name = "bot_id", referencedColumnName = "id")
    private Bot bot;
    
    private String title;
    private String content;
    private String description;
    
    @Column(name = "image_url")
    private String imageUrl;
    
    @Column(name = "published_at")
    private LocalDateTime publishedAt;
    
    private boolean processed;
    private boolean posted;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();



    
}
