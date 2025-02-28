package com.egemen.TweetBotTelegram.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Entity
@Table(name = "process_status")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProcessStatus {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "bot_id")
    private Bot bot;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "process_name", nullable = false)
    private String processName;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "last_attempt")
    private Timestamp lastAttempt;

    @Column(name = "retries")
    private int retries;

    @Column(name = "result")
    private String result;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "task_id")
    private int taskId;

    @Column(name = "created_at")
    private Timestamp createdAt;
}
