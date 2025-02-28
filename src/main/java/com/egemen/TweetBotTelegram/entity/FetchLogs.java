package com.egemen.TweetBotTelegram.entity;

import com.egemen.TweetBotTelegram.enums.FetchStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "fetch_logs")
public class FetchLogs {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "bot_id")
    private Bot bot;

    @Column(name = "fetched_at")
    private Timestamp fetchedAt;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private FetchStatus status;

    @Column(name = "fetched_count")
    private int fetchedCount;

    public FetchLogs(Bot bot, Timestamp timestamp, FetchStatus fetchStatus, int fetchedCount) {
        this.bot = bot;
        this.fetchedAt = timestamp;
        this.status = fetchStatus;
        this.fetchedCount = fetchedCount;
    }

}