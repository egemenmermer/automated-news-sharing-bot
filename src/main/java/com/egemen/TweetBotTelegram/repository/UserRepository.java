package com.egemen.TweetBotTelegram.repository;

import com.egemen.TweetBotTelegram.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findById(Long id);
    List<User> findAll();
    Optional<User> findByUsername(String username);
} 