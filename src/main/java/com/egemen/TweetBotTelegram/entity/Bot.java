package com.egemen.TweetBotTelegram.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.sql.Timestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "bots")
@Data
public class Bot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Column(unique = true,nullable = false, name = "name")
    private String name;

    @Column(name = "last_run")
    private Timestamp lastRun;
}

