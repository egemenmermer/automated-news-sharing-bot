package com.egemen.TweetBotTelegram.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "post_logs")
public class PostLogs {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "bot_id")
    private Bot bot;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "post_count")
    private Integer postCount;

    @Column(name = "posted_at")
    private Timestamp postedAt;

    @Column(name = "scheduled_at")
    private Timestamp scheduledAt;
}
