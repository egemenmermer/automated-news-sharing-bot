package com.egemen.TweetBotTelegram.controller;

import com.egemen.TweetBotTelegram.entity.Bot;
import com.egemen.TweetBotTelegram.service.BotService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/bots")
@RequiredArgsConstructor
public class BotController {
    private final BotService botService;

    @PostMapping
    public ResponseEntity<Bot> createBot(@RequestBody Bot bot) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(botService.createBot(bot));
    }

    @PostMapping("/{chatId}/start")
    public ResponseEntity<Void> startBot(@PathVariable String chatId) {
        botService.startBot(chatId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{chatId}/stop")
    public ResponseEntity<Void> stopBot(@PathVariable String chatId) {
        botService.stopBot(chatId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/status")
    public ResponseEntity<Boolean> getBotStatus(@PathVariable String chatId) {
        return ResponseEntity.ok(botService.isRunning(chatId));
    }
}
