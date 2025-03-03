package com.egemen.TweetBotTelegram.service.Impl;

import com.egemen.TweetBotTelegram.entity.News;
import com.egemen.TweetBotTelegram.service.GeminiService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;

@Slf4j
@Service
public class GeminiServiceImpl implements GeminiService {

    @Value("${GEMINI_API_KEY}")
    private String apiKey;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-pro:generateContent";
    private static final long INITIAL_RETRY_DELAY = 5000L; // 5 seconds
    private static final int MAX_RETRIES = 5;
    private static final double BACKOFF_MULTIPLIER = 2.0;
    private static final int MAX_CONCURRENT_REQUESTS = 2;
    private final Semaphore requestSemaphore = new Semaphore(MAX_CONCURRENT_REQUESTS);

    // Simple in-memory cache for responses
    private final Map<String, String> responseCache = new HashMap<>();
    private static final int CACHE_SIZE_LIMIT = 100;
    private static final long CACHE_EXPIRY_MS = 3600000; // 1 hour
    private final Map<String, Long> cacheTimestamps = new HashMap<>();

    public GeminiServiceImpl() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public String generateImagePrompt(String title, String content) throws Exception {
        String prompt = String.format(
            "Create a visually descriptive prompt for generating an image that represents this news article. " +
            "The prompt should be detailed and focus on the key visual elements. " +
            "Keep it under 100 words and make it suitable for an image generation AI.\n\n" +
            "Title: %s\n\n" +
            "Content: %s",
            title, content
        );

        return generateResponse(prompt);
    }

    @Override
    public String generateSummary(String title, String content) throws Exception {
        String prompt = String.format(
            "Create an engaging and concise summary of this news article suitable for an Instagram caption. " +
            "Keep it under 200 characters, make it attention-grabbing, and maintain a professional tone.\n\n" +
            "Title: %s\n\n" +
            "Content: %s",
            title, content
        );

        return generateResponse(prompt);
    }

    @Override
    public String generateResponse(String prompt) throws Exception {
        int retryCount = 0;
        long retryDelay = INITIAL_RETRY_DELAY;

        // Check cache first
        String cacheKey = prompt.trim();
        if (responseCache.containsKey(cacheKey)) {
            Long timestamp = cacheTimestamps.get(cacheKey);
            if (timestamp != null && System.currentTimeMillis() - timestamp < CACHE_EXPIRY_MS) {
                log.info("Using cached response for prompt");
                return responseCache.get(cacheKey);
            } else {
                // Cache entry expired
                responseCache.remove(cacheKey);
                cacheTimestamps.remove(cacheKey);
            }
        }

        try {
            requestSemaphore.acquire(); // Acquire a permit before making the request
            while (retryCount <= MAX_RETRIES) {
                try {
                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    
                    Map<String, Object> textPart = new HashMap<>();
                    textPart.put("text", prompt);

                    Map<String, Object> message = new HashMap<>();
                    message.put("role", "user");
                    message.put("parts", List.of(textPart));

                    Map<String, Object> requestBody = new HashMap<>();
                    requestBody.put("contents", List.of(message));
                    requestBody.put("safetySettings", List.of(
                        Map.of(
                            "category", "HARM_CATEGORY_HARASSMENT",
                            "threshold", "BLOCK_NONE"
                        ),
                        Map.of(
                            "category", "HARM_CATEGORY_HATE_SPEECH",
                            "threshold", "BLOCK_NONE"
                        ),
                        Map.of(
                            "category", "HARM_CATEGORY_SEXUALLY_EXPLICIT",
                            "threshold", "BLOCK_NONE"
                        ),
                        Map.of(
                            "category", "HARM_CATEGORY_DANGEROUS_CONTENT",
                            "threshold", "BLOCK_NONE"
                        )
                    ));

                    String url = GEMINI_API_URL + "?key=" + apiKey;
                    
                    HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
                    ResponseEntity<String> response = restTemplate.exchange(
                        url,
                        HttpMethod.POST,
                        request,
                        String.class
                    );

                    if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                        JsonNode jsonResponse = objectMapper.readTree(response.getBody());
                        String result = extractTextFromResponse(jsonResponse);
                        
                        // Cache the result
                        if (result != null && !result.isEmpty()) {
                            // Manage cache size
                            if (responseCache.size() >= CACHE_SIZE_LIMIT) {
                                // Remove oldest entry
                                String oldestKey = cacheTimestamps.entrySet().stream()
                                    .min(Map.Entry.comparingByValue())
                                    .map(Map.Entry::getKey)
                                    .orElse(null);
                                if (oldestKey != null) {
                                    responseCache.remove(oldestKey);
                                    cacheTimestamps.remove(oldestKey);
                                }
                            }
                            responseCache.put(cacheKey, result);
                            cacheTimestamps.put(cacheKey, System.currentTimeMillis());
                        }
                        
                        return result;
                    }

                    throw new Exception("Failed to generate content from Gemini API");
                } catch (Exception e) {
                    if (e.getMessage() != null && e.getMessage().contains("429")) {
                        if (retryCount < MAX_RETRIES) {
                            log.warn("Rate limit exceeded, retrying in {} ms (attempt {}/{})", 
                                retryDelay, retryCount + 1, MAX_RETRIES);
                            Thread.sleep(retryDelay);
                            retryDelay *= BACKOFF_MULTIPLIER;
                            retryCount++;
                            continue;
                        }
                    }
                    log.error("Error generating content from Gemini API: {}", e.getMessage());
                    throw new Exception("Failed to generate content: " + e.getMessage());
                }
            }
            throw new Exception("Failed to generate content after " + MAX_RETRIES + " retries");
        } finally {
            requestSemaphore.release(); // Always release the permit
        }
    }

    private String extractTextFromResponse(JsonNode response) {
        try {
            return response
                .path("candidates")
                .get(0)
                .path("content")
                .path("parts")
                .get(0)
                .path("text")
                .asText()
                .trim();
        } catch (Exception e) {
            log.error("Error extracting text from Gemini API response: {}", e.getMessage());
            return "";
        }
    }

    @Override
    public String generateImageForNews(News news) throws Exception {
        String imagePrompt = generateImagePrompt(news.getTitle(), news.getContent());
        // For now, return a placeholder URL. In a real implementation,
        // this would integrate with an image generation service
        return "generated-images/placeholder.png";
    }

    @Override
    public String generateInstagramCaption(News news) throws Exception {
        String summary = generateSummary(news.getTitle(), news.getContent());
        return String.format("%s\n\nSource: %s\n\n#news #ai #neuralNews", summary, news.getSource());
    }
}