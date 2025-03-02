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

    @Column(unique = true, nullable = false, name = "name")
    private String name;

    @Column(name = "instagram_username")
    private String instagramUsername;

    @Column(name = "instagram_password")
    private String instagramPassword;

    @Column(name = "instagram_access_token")
    private String instagramAccessToken;

    @Column(name = "instagram_user_id")
    private String instagramUserId;

    @Column(name = "pexels_api_key")
    private String pexelsApiKey;

    @Column(name = "mediastack_api_key")
    private String mediastackApiKey;

    @Column(name = "gemini_api_key")
    private String geminiApiKey;

    @Column(name = "telegram_bot_username")
    private String telegramBotUsername;

    @Column(name = "telegram_bot_token")
    private String telegramBotToken;

    @Column(name = "fetch_time", nullable = false)
    private Timestamp fetchTime;

    @Column(name = "post_time", nullable = false)
    private Timestamp postTime;

    @Column(name = "last_run")
    private Timestamp lastRun;

    // Temporarily commenting out is_active field due to database schema mismatch
    // @Column(name = "is_active", nullable = false)
    // private Boolean isActive = true;

    // Temporarily commenting out is_fetch_turkish field due to database schema mismatch
    // @Column(name = "is_fetch_turkish", nullable = false)
    // private Boolean isFetchTurkish = false;
}
