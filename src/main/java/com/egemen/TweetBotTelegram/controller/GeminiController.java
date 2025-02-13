package com.egemen.TweetBotTelegram.controller;

import com.egemen.TweetBotTelegram.service.GeminiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/gemini")
@RequiredArgsConstructor
public class GeminiController {

    private final GeminiService geminiService;

    @PostMapping("/summarize-news")
    public ResponseEntity<String> summarizeNews(
            @RequestParam String title,
            @RequestParam String content) {
        return ResponseEntity.ok(geminiService.summarizeNews(title, content));
    }

    @PostMapping("/generate-tweet")
    public ResponseEntity<String> generateTweet(@RequestParam String summary) {
        return ResponseEntity.ok(geminiService.generateTweet(summary));
    }

    @PostMapping("/generate-summary")
    public ResponseEntity<String> generateSummary(
            @RequestParam String title,
            @RequestParam String content) {
        return ResponseEntity.ok(geminiService.generateSummary(title, content));
    }

    @PostMapping("/generate-image-prompt")
    public ResponseEntity<String> generateImagePrompt(
            @RequestParam String title,
            @RequestParam String content) {
        return ResponseEntity.ok(geminiService.generateImagePrompt(title, content));
    }

    @PostMapping("/generate-caption")
    public ResponseEntity<String> generateCaption(
            @RequestParam String title,
            @RequestParam String content) {
        return ResponseEntity.ok(geminiService.generateCaption(title, content));
    }

    @PostMapping("/generate")
    public ResponseEntity<String> generateResponse(@RequestParam String prompt) {
        return ResponseEntity.ok(geminiService.generateResponse(prompt));
    }
} 