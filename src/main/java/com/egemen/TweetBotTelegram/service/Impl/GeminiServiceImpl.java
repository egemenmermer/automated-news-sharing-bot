package com.egemen.TweetBotTelegram.service.Impl;

import com.egemen.TweetBotTelegram.service.GeminiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class GeminiServiceImpl implements GeminiService {
    private static final Logger log = LoggerFactory.getLogger(GeminiServiceImpl.class);
    
    private final RestTemplate restTemplate;
    private final String apiKey;

    public GeminiServiceImpl(
            RestTemplate restTemplate,
            @Value("${gemini.api.key}") String apiKey) {
        this.restTemplate = restTemplate;
        this.apiKey = apiKey;
    }

    // Uses Google's Gemini AI for:
    // 1. Summarizing news articles
    // 2. Generating engaging tweets
    
    @Override
    public String summarizeNews(String newsTitle, String newsContent) {
        // Creates concise summaries of news articles
        String GEMINI_ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + apiKey;

        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");

        Map<String, Object> requestBody = new HashMap<>();
        List<Map<String, Object>> contents = new ArrayList<>();
        Map<String, Object> content = new HashMap<>();
        content.put("text", "Summarize this news in one or two sentences: " + newsTitle + " - " + newsContent);
        contents.add(content);
        requestBody.put("contents", Collections.singletonList(Map.of("parts", contents)));

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(GEMINI_ENDPOINT, entity, Map.class);

        if (response.getBody() != null && response.getBody().containsKey("candidates")) {
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.getBody().get("candidates");
            if (!candidates.isEmpty()) {
                Map<String, Object> contentResponse = (Map<String, Object>) candidates.get(0).get("content");
                List<Map<String, Object>> parts = (List<Map<String, Object>>) contentResponse.get("parts");
                return parts.get(0).get("text").toString();
            }
        }
        return "Error generating summary.";
    }

    @Override
    public String generateTweet(String newsSummary) {
        // Generates social media-friendly tweets
        String GEMINI_ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + apiKey;

        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");

        Map<String, Object> requestBody = new HashMap<>();
        List<Map<String, Object>> contents = new ArrayList<>();
        Map<String, Object> content = new HashMap<>();
        content.put("text", "Generate a short, engaging tweet (maximum 280 characters) about this news: " + newsSummary);
        contents.add(content);
        requestBody.put("contents", Collections.singletonList(Map.of("parts", contents)));

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(GEMINI_ENDPOINT, entity, Map.class);

        if (response.getBody() != null && response.getBody().containsKey("candidates")) {
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.getBody().get("candidates");
            if (!candidates.isEmpty()) {
                Map<String, Object> contentResponse = (Map<String, Object>) candidates.get(0).get("content");
                List<Map<String, Object>> parts = (List<Map<String, Object>>) contentResponse.get("parts");
                return parts.get(0).get("text").toString();
            }
        }
        return "Error generating tweet.";
    }

    @Override
    public String generateSummary(String title, String content) {
        return summarizeNews(title, content);
    }

    @Override
    public String generateImagePrompt(String title, String content) {
        try {
            log.info("Generating image prompt for title: {}", title);
            return generateResponse("Generate an image prompt for: " + title);
        } catch (Exception e) {
            log.error("Error generating image prompt: {}", e.getMessage());
            return "Error generating image prompt";
        }
    }

    @Override
    public String generateCaption(String title, String content) {
        try {
            log.info("Generating caption for title: {}", title);
            return generateResponse("Generate a caption for: " + title);
        } catch (Exception e) {
            log.error("Error generating caption: {}", e.getMessage());
            return "Error generating caption";
        }
    }

    @Override
    public String generateResponse(String prompt) {
        try {
            log.info("Generating response for prompt: {}", prompt);
            // TODO: Implement actual Gemini API call
            return "Sample response for: " + prompt;
        } catch (Exception e) {
            log.error("Error generating response: {}", e.getMessage());
            return "Error generating response";
        }
    }
}