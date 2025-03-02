package com.egemen.TweetBotTelegram.entity;

import com.egemen.TweetBotTelegram.enums.SummarizedStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "summarized_news")
public class SummarizedNews {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "bot_id")
    private Bot bot;

    @ManyToOne
    @JoinColumn(name = "news_id")
    private News news;

    @Column(name = "summary")
    private String summary;

    @Column(name = "summarized_at")
    private Timestamp summarizedAt;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private SummarizedStatus status;

    @Column(name = "summarized_count")
    private int summarizedCount;
}
