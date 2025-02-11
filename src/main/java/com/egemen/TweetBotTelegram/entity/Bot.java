package com.egemen.TweetBotTelegram.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "bots")
public class Bot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "api_key")
    private String apiKey;

    @Column(name = "api_secret")
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

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getApiSecret() {
        return apiSecret;
    }

    public void setApiSecret(String apiSecret) {
        this.apiSecret = apiSecret;
    }

    public String getInstagramUsername() {
        return instagramUsername;
    }

    public void setInstagramUsername(String instagramUsername) {
        this.instagramUsername = instagramUsername;
    }

    public String getInstagramPassword() {
        return instagramPassword;
    }

    public void setInstagramPassword(String instagramPassword) {
        this.instagramPassword = instagramPassword;
    }

    public String getInstagramAccessToken() {
        return instagramAccessToken;
    }

    public void setInstagramAccessToken(String instagramAccessToken) {
        this.instagramAccessToken = instagramAccessToken;
    }

    public String getPexelsApiKey() {
        return pexelsApiKey;
    }

    public void setPexelsApiKey(String pexelsApiKey) {
        this.pexelsApiKey = pexelsApiKey;
    }

    public String getMediastackApiKey() {
        return mediastackApiKey;
    }

    public void setMediastackApiKey(String mediastackApiKey) {
        this.mediastackApiKey = mediastackApiKey;
    }

    public LocalDateTime getFetchTime() {
        return fetchTime;
    }

    public void setFetchTime(LocalDateTime fetchTime) {
        this.fetchTime = fetchTime;
    }

    public LocalDateTime getPostTime() {
        return postTime;
    }

    public void setPostTime(LocalDateTime postTime) {
        this.postTime = postTime;
    }

    public LocalDateTime getLastRun() {
        return lastRun;
    }

    public void setLastRun(LocalDateTime lastRun) {
        this.lastRun = lastRun;
    }
}

