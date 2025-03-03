package com.egemen.TweetBotTelegram.entity;

import com.egemen.TweetBotTelegram.enums.FetchStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Entity
@Table(name = "fetch_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FetchLogs {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "bot_id", nullable = false)
    private Bot bot;

    private Timestamp fetchTime;

    @Enumerated(EnumType.STRING)
    private FetchStatus status;

    private Integer articleCount;

    private String errorMessage;

    // Constructor with all fields except id
    public FetchLogs(Bot bot, Timestamp fetchTime, FetchStatus status, Integer articleCount) {
        this.bot = bot;
        this.fetchTime = fetchTime;
        this.status = status;
        this.articleCount = articleCount;
    }

    // Constructor with all fields except id and errorMessage
    public FetchLogs(Bot bot, Timestamp fetchTime, FetchStatus status, Integer articleCount, String errorMessage) {
        this.bot = bot;
        this.fetchTime = fetchTime;
        this.status = status;
        this.articleCount = articleCount;
        this.errorMessage = errorMessage;
    }

    // Add getters and setters for all fields
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Bot getBot() {
        return bot;
    }

    public void setBot(Bot bot) {
        this.bot = bot;
    }

    public Timestamp getFetchTime() {
        return fetchTime;
    }

    public void setFetchTime(Timestamp fetchTime) {
        this.fetchTime = fetchTime;
    }

    public FetchStatus getStatus() {
        return status;
    }

    public void setStatus(FetchStatus status) {
        this.status = status;
    }

    public Integer getArticleCount() {
        return articleCount;
    }

    public void setArticleCount(Integer articleCount) {
        this.articleCount = articleCount;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}