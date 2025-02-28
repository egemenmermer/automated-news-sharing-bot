package com.egemen.TweetBotTelegram.service.Impl;

import com.egemen.TweetBotTelegram.entity.Bot;
import com.egemen.TweetBotTelegram.entity.News;
import com.egemen.TweetBotTelegram.entity.User;
import com.egemen.TweetBotTelegram.exception.CustomException;
import com.egemen.TweetBotTelegram.repository.BotRepository;
import com.egemen.TweetBotTelegram.repository.NewsRepository;
import com.egemen.TweetBotTelegram.repository.UserRepository;
import com.egemen.TweetBotTelegram.service.BotService;
import com.egemen.TweetBotTelegram.service.NewsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Implementation of BotService responsible for managing automated bots.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BotServiceImpl implements BotService {

    @Autowired
    private BotRepository botRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    public Bot createBot(Bot bot) {
        if(bot.getUser() == null || bot.getName() == null) {
            throw new CustomException(new Exception("User and Bot Name are required"), HttpStatus.BAD_REQUEST);
        }
        try {
            return botRepository.save(bot);
        } catch (DataIntegrityViolationException e) {
            throw new CustomException(e, HttpStatus.BAD_REQUEST);
        }catch(Exception e) {
            throw new CustomException(new Exception("Error creating bot" + e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }

    @Override
    public List<Bot> listBots(Long userId) {
        Optional<User> user = userRepository.findById(userId);
        if(user.isEmpty()) {
            throw new CustomException(new Exception("User not found"), HttpStatus.NOT_FOUND);
        }
        List<Bot> bots = botRepository.findAllByUser(user.get());
        if(bots.isEmpty()) {
            throw new CustomException(new Exception("No Bots found"), HttpStatus.NOT_FOUND);
        }
        return bots;
    }

    @Override
    public void saveBot(Bot bot) {
        try {
            botRepository.save(bot);
        } catch (Exception e) {
            throw new CustomException(e, HttpStatus.BAD_REQUEST);
        }
    }

    @Override
    public Bot getBotByUserId(Long userId) {
        Optional<User> user = userRepository.findById(userId);
        if(user.isEmpty()) {
            throw new CustomException(new Exception("User not found"), HttpStatus.NOT_FOUND);
        }
        return user.map(value -> botRepository.findByUser(value)).orElseThrow(() -> new CustomException(new Exception("Bot not found"), HttpStatus.NOT_FOUND));
    }

    @Override
    public Bot getBotById(Long botId) {
        return botRepository.findById(botId).orElseThrow(() -> new CustomException(new Exception("Bot not found"), HttpStatus.NOT_FOUND));
    }
}