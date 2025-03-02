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
@Table(name = "process_status")
public class ProcessStatus {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "bot_id")
    private Bot bot;

    @Column(name = "process_name", nullable = false)
    private String processName;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "task_id")
    private Integer taskId;

    @Column(name = "retries")
    private Integer retries = 0;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "result")
    private String result;

    @Column(name = "created_at")
    private Timestamp createdAt;

    @Column(name = "last_attempt")
    private Timestamp lastAttempt;
}
