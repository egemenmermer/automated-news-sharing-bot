package com.egemen.TweetBotTelegram.controller;

import com.egemen.TweetBotTelegram.service.GeminiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/gemini")
public class GeminiController {

    private final GeminiService geminiService;

    public GeminiController(GeminiService geminiService) {
        this.geminiService = geminiService;
    }

    @PostMapping("/generate-summary")
    public ResponseEntity<?> generateSummary(@RequestBody Map<String, String> request) {
        try {
            String title = request.get("title");
            String content = request.get("content");
            
            if (title == null || content == null) {
                return ResponseEntity.badRequest().body("Title and content are required");
            }

            String summary = geminiService.generateSummary(title, content);
            return ResponseEntity.ok(Map.of("summary", summary));
        } catch (Exception e) {
            log.error("Error generating summary: {}", e.getMessage());
            return ResponseEntity.internalServerError().body("Error generating summary: " + e.getMessage());
        }
    }

    @PostMapping("/generate-image-prompt")
    public ResponseEntity<?> generateImagePrompt(@RequestBody Map<String, String> request) {
        try {
            String title = request.get("title");
            String content = request.get("content");
            
            if (title == null || content == null) {
                return ResponseEntity.badRequest().body("Title and content are required");
            }

            String imagePrompt = geminiService.generateImagePrompt(title, content);
            return ResponseEntity.ok(Map.of("imagePrompt", imagePrompt));
        } catch (Exception e) {
            log.error("Error generating image prompt: {}", e.getMessage());
            return ResponseEntity.internalServerError().body("Error generating image prompt: " + e.getMessage());
        }
    }

    @PostMapping("/generate")
    public ResponseEntity<?> generateResponse(@RequestBody Map<String, String> request) {
        try {
            String prompt = request.get("prompt");
            
            if (prompt == null) {
                return ResponseEntity.badRequest().body("Prompt is required");
            }

            String response = geminiService.generateResponse(prompt);
            return ResponseEntity.ok(Map.of("response", response));
        } catch (Exception e) {
            log.error("Error generating response: {}", e.getMessage());
            return ResponseEntity.internalServerError().body("Error generating response: " + e.getMessage());
        }
    }
}