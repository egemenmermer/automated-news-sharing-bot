package com.egemen.TweetBotTelegram.entity;

import com.egemen.TweetBotTelegram.enums.PostStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.time.LocalDateTime;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "post_logs")
public class PostLogs {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "bot_id")
    private Bot bot;

    @Column(name = "scheduled_at")
    private Timestamp scheduledAt;

    @Column(name = "posted_at")
    private Timestamp postedAt;

    @ManyToOne
    @JoinColumn(name = "posted_tweet_id")
    private Tweet postedTweet;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private PostStatus postStatus;

    @Column(name = "post_count")
    private int postCount;
}
