package com.egemen.TweetBotTelegram.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.sql.Timestamp;

@Entity
@Table(name = "bots")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Bot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "created_at")
    private Timestamp createdAt;

    private String telegramBotUsername;
    
    private String telegramBotToken;
    
    private String instagramUsername;
    
    private String instagramPassword;
    
    private String instagramUserId;
    
    @Column(columnDefinition = "TEXT")
    private String instagramAccessToken;
    
    private String mediastackApiKey;
    
    private String pexelsApiKey;
    
    private String geminiApiKey;
    
    @Column(name = "fetch_time")
    private String fetchTime;
    
    @Column(name = "post_time")
    private String postTime;
    
    @Column(name = "last_run")
    private Timestamp lastRun;

    // Temporarily commenting out is_active field due to database schema mismatch
    // @Column(name = "is_active", nullable = false)
    // private Boolean isActive = true;

    // Temporarily commenting out is_fetch_turkish field due to database schema mismatch
    // @Column(name = "is_fetch_turkish", nullable = false)
    // private Boolean isFetchTurkish = false;
}
