package com.egemen.TweetBotTelegram.service;

import com.egemen.TweetBotTelegram.entity.User;

import java.util.List;
import java.util.Optional;

public interface UserService {
    List<User> getAllUsers();
    User createUser(User user);
    Optional<User> getUserByUsername(String username);
}
