package com.egemen.TweetBotTelegram.service.Impl;

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

@Slf4j
@Service
public class GeminiServiceImpl implements GeminiService {

    @Value("${GEMINI_API_KEY}")
    private String apiKey;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1/models/gemini-pro/generateContent";

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
                return extractTextFromResponse(jsonResponse);
            }

            throw new Exception("Failed to generate content from Gemini API");
        } catch (Exception e) {
            log.error("Error generating content from Gemini API: {}", e.getMessage());
            throw new Exception("Failed to generate content: " + e.getMessage());
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
}