package com.egemen.TweetBotTelegram.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "bots")
@Data
public class Bot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @Column(name = "api_key", nullable = false)
    private String apiKey;

    @Column(name = "api_secret", nullable = false)
    private String apiSecret;

    @Column(name = "instagram_username")
    private String instagramUsername;

    @Column(name = "instagram_password")
    private String instagramPassword;

    @Column(name = "instagram_access_token")
    private String instagramAccessToken;

    @Column(name = "pexels_api_key")
    private String pexelsApiKey;

    @Column(name = "mediastack_api_key")
    private String mediastackApiKey;

    @Column(name = "fetch_time")
    private LocalDateTime fetchTime;

    @Column(name = "post_time")
    private LocalDateTime postTime;

    @Column(name = "last_run")
    private LocalDateTime lastRun;
}

