package com.egemen.TweetBotTelegram.service.Impl;

import com.egemen.TweetBotTelegram.entity.News;
import com.egemen.TweetBotTelegram.service.GeminiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class GeminiServiceImpl implements GeminiService {

    private final String apiKey;
    private final RestTemplate restTemplate;
    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-pro:generateContent";

    @Autowired
    public GeminiServiceImpl(String geminiApiKey) {
        this.apiKey = geminiApiKey;
        this.restTemplate = new RestTemplate();
        log.info("GeminiServiceImpl initialized with API key");
    }

    @Override
    public String generateImagePrompt(String title, String content) throws Exception {
        String prompt = "Create a detailed image prompt for a news article with the following title and content. " +
                "The prompt should describe a realistic scene that represents the article's main topic. " +
                "Focus on visual elements, lighting, mood, and composition. Keep it under 100 words:\n\n" +
                "Title: " + title + "\n\n" +
                "Content: " + content;
        
        try {
            return generateResponse(prompt);
        } catch (Exception e) {
            log.warn("Error generating image prompt with Gemini API, using fallback: {}", e.getMessage());
            // Fallback: Generate a simple image prompt based on the title
            return generateFallbackImagePrompt(title);
        }
    }

    @Override
    public String generateSummary(String title, String content) throws Exception {
        String prompt = "Summarize the following news article in a concise, engaging way suitable for a social media post. " +
                "Include relevant hashtags at the end. Keep it under 200 characters:\n\n" +
                "Title: " + title + "\n\n" +
                "Content: " + content;
        
        try {
            return generateResponse(prompt);
        } catch (Exception e) {
            log.warn("Error generating summary with Gemini API, using fallback: {}", e.getMessage());
            // Fallback: Generate a simple summary based on the title
            return generateFallbackSummary(title);
        }
    }

    @Override
    public String generateResponse(String prompt) throws Exception {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            Map<String, Object> requestBody = new HashMap<>();
            Map<String, Object> content = new HashMap<>();
            Map<String, Object> part = new HashMap<>();
            part.put("text", prompt);
            content.put("parts", List.of(part));
            requestBody.put("contents", List.of(content));
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            String url = GEMINI_API_URL + "?key=" + apiKey;
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                List<Map<String, Object>> candidates = (List<Map<String, Object>>) responseBody.get("candidates");
                if (candidates != null && !candidates.isEmpty()) {
                    Map<String, Object> candidate = candidates.get(0);
                    List<Map<String, Object>> parts = (List<Map<String, Object>>) 
                        ((Map<String, Object>) candidate.get("content")).get("parts");
                    if (parts != null && !parts.isEmpty()) {
                        return (String) parts.get(0).get("text");
                    }
                }
            }
            
            throw new Exception("Failed to generate response from Gemini API");
        } catch (Exception e) {
            log.error("Error generating response from Gemini API: {}", e.getMessage());
            throw new Exception("Error generating response: " + e.getMessage());
        }
    }

    @Override
    public String generateImageForNews(News news) throws Exception {
        try {
            return generateImagePrompt(news.getTitle(), news.getContent());
        } catch (Exception e) {
            log.warn("Error generating image for news with Gemini API, using fallback: {}", e.getMessage());
            return generateFallbackImagePrompt(news.getTitle());
        }
    }

    @Override
    public String generateInstagramCaption(News news) throws Exception {
        try {
            return generateSummary(news.getTitle(), news.getContent());
        } catch (Exception e) {
            log.warn("Error generating Instagram caption with Gemini API, using fallback: {}", e.getMessage());
            return generateFallbackSummary(news.getTitle());
        }
    }
    
    // Fallback methods
    private String generateFallbackImagePrompt(String title) {
        return "A professional news image related to: " + title;
    }
    
    private String generateFallbackSummary(String title) {
        // Extract hashtags from the title
        String[] words = title.split("\\s+");
        StringBuilder hashtags = new StringBuilder();
        for (String word : words) {
            if (word.length() > 4) {
                hashtags.append("#").append(word.replaceAll("[^a-zA-Z0-9]", "")).append(" ");
            }
        }
        
        return title + " " + hashtags.toString().trim();
    }

    private String generateResponseWithRetry(String prompt, int maxRetries) {
        int retryCount = 0;
        long retryDelay = 1000; // Start with 1 second delay
        
        while (retryCount < maxRetries) {
            try {
                return generateResponse(prompt);
            } catch (Exception e) {
                if (e.getMessage().contains("429") || e.getMessage().contains("RESOURCE_EXHAUSTED")) {
                    retryCount++;
                    if (retryCount >= maxRetries) {
                        log.warn("Max retries reached for Gemini API, using fallback");
                        return getFallbackResponse(prompt);
                    }
                    
                    log.info("Rate limit hit, retrying in {} ms (attempt {}/{})", retryDelay, retryCount, maxRetries);
                    try {
                        Thread.sleep(retryDelay);
                        // Exponential backoff
                        retryDelay *= 2;
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return getFallbackResponse(prompt);
                    }
                } else {
                    // Not a rate limit error, use fallback
                    log.error("Error generating response from Gemini API: {}", e.getMessage(), e);
                    return getFallbackResponse(prompt);
                }
            }
        }
        
        return getFallbackResponse(prompt);
    }

    private String getFallbackResponse(String prompt) {
        // Simple fallback logic
        if (prompt.contains("image") || prompt.contains("picture")) {
            return "A professional news image related to: " + prompt.substring(0, Math.min(prompt.length(), 100));
        } else if (prompt.contains("summarize") || prompt.contains("summary")) {
            return "Brief summary of the news article.";
        } else {
            return "Generated content unavailable at this time.";
        }
    }
}