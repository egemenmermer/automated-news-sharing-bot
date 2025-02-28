package com.egemen.TweetBotTelegram.service.Impl;

import com.egemen.TweetBotTelegram.entity.News;
import com.egemen.TweetBotTelegram.entity.SummarizedNews;
import com.egemen.TweetBotTelegram.entity.Tweet;
import com.egemen.TweetBotTelegram.enums.NewsStatus;
import com.egemen.TweetBotTelegram.enums.SummarizedStatus;
import com.egemen.TweetBotTelegram.enums.TweetStatus;
import com.egemen.TweetBotTelegram.repository.NewsRepository;
import com.egemen.TweetBotTelegram.repository.SummarizedNewsRepository;
import com.egemen.TweetBotTelegram.repository.TweetsRepository;
import com.egemen.TweetBotTelegram.service.GeminiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class GeminiServiceImpl implements GeminiService {
    private String geminiApiKey="AIzaSyDZ77gvYg_yKJiMgkFUtRjPQh4xTmmJnL4";
    private final String  GEMINI_ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent";
    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";

    private static final Logger logger = LoggerFactory.getLogger(GeminiServiceImpl.class);

    private LocalDateTime startTime = LocalDateTime.now();


    @Value("${gemini.client.id}")
    private String clientId;
    @Value("${gemini.client.secret}")
    private String clientSecret;

    @Autowired
    private NewsRepository newsRepository;
    @Autowired
    private SummarizedNewsRepository summarizedNewsRepository;
    @Autowired
    private TweetsRepository tweetsRepository;

    @Override
    public void start() throws InterruptedException {
        refreshAccessToken();
        summarize();
        generate();
    }
    @Override
    public String summarizeNews(String newsTitle, String newsContent) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");

        // Prepare request body to match the working curl structure
        Map<String, Object> requestBody = new HashMap<>();
        List<Map<String, Object>> contents = new ArrayList<>();
        Map<String, Object> contentItem = new HashMap<>();

        List<Map<String, Object>> parts = new ArrayList<>();
        Map<String, Object> textPart = new HashMap<>();
        textPart.put("text", "Summarize this news in one or two sentences, translate and write it only inde Turkish: " + newsTitle + " - " + newsContent);
        parts.add(textPart);

        contentItem.put("parts", parts);
        contents.add(contentItem);
        requestBody.put("contents", contents);

        // Build the URL with API key
        String url = GEMINI_ENDPOINT + "?key=" + geminiApiKey;

        // Prepare HTTP entity
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        // Create RestTemplate with error handler
        RestTemplate restTemplate = new RestTemplate();

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

            // Log response for debugging
            System.out.println("Response: " + response.getBody());

            // Check for valid response
            if (response.getBody() != null && response.getBody().containsKey("candidates")) {
                List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.getBody().get("candidates");
                if (!candidates.isEmpty()) {
                    Map<String, Object> contentResponse = (Map<String, Object>) candidates.get(0).get("content");
                    List<Map<String, Object>> partsList = (List<Map<String, Object>>) contentResponse.get("parts");
                    if (!partsList.isEmpty()) {
                        return partsList.get(0).get("text").toString();
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error calling Gemini API: " + e.getMessage());
            return "Error generating summary: " + e.getMessage();
        }

        return "Error generating summary.";
    }




    @Override
    public String generateTweet(String newsSummary) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");

        // Prepare request body to match the working structure
        Map<String, Object> requestBody = new HashMap<>();
        List<Map<String, Object>> contents = new ArrayList<>();
        Map<String, Object> contentItem = new HashMap<>();

        List<Map<String, Object>> parts = new ArrayList<>();
        Map<String, Object> textPart = new HashMap<>();
        textPart.put("text", "Generate a short, engaging tweet (maximum 280 characters) about this news only in Turkish(if not): " + newsSummary);
        parts.add(textPart);

        contentItem.put("parts", parts);
        contents.add(contentItem);
        requestBody.put("contents", contents);

        // Build the URL with API key
        String url = GEMINI_ENDPOINT + "?key=" + geminiApiKey;

        // Prepare HTTP entity
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        // Create RestTemplate
        RestTemplate restTemplate = new RestTemplate();

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

            // Log response for debugging
            System.out.println("Response: " + response.getBody());

            // Check for valid response
            if (response.getBody() != null && response.getBody().containsKey("candidates")) {
                List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.getBody().get("candidates");
                if (!candidates.isEmpty()) {
                    Map<String, Object> contentResponse = (Map<String, Object>) candidates.get(0).get("content");
                    List<Map<String, Object>> partsList = (List<Map<String, Object>>) contentResponse.get("parts");
                    if (!partsList.isEmpty()) {
                        return partsList.get(0).get("text").toString();
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error calling Gemini API: " + e.getMessage());
            return "Error generating tweet: " + e.getMessage();
        }

        return "Error generating tweet.";
    }


    @Override

    public List<SummarizedNews> summarize() throws InterruptedException {
        List<News> newsList = newsRepository.findByStatus(NewsStatus.NOT_SUMMARIZED);
        if (newsList.isEmpty()) {
            throw new RuntimeException("No news to summarize found.");
        }
        for (News news : newsList) {
            if (news.isProcessed()) {
                continue;
            }
            String summary = summarizeNews(news.getTitle(), news.getContent());
            SummarizedNews summarizedNews = new SummarizedNews();
            summarizedNews.setBot(news.getBot());
            summarizedNews.setNews(news);
            summarizedNews.setSummarizedAt(Timestamp.valueOf(LocalDateTime.now()));

            if (!summary.equals("Error generating summary.")) {
                summarizedNews.setContent(summary);
                summarizedNews.setStatus(SummarizedStatus.SUMMARIZED);
                news.setStatus(NewsStatus.SUMMARIZED);
                news.setProcessed(true);
            } else {
                summarizedNews.setContent(null);
                summarizedNews.setStatus(SummarizedStatus.FAILED);
            }
            summarizedNewsRepository.save(summarizedNews);
            newsRepository.save(news);
            Thread.sleep(300);
        }
        return summarizedNewsRepository.findByStatus(SummarizedStatus.SUMMARIZED);
    }

    @Override
    public List<Tweet> generate() throws InterruptedException {
        List<SummarizedNews> summarizedNewsList = summarizedNewsRepository.findByStatus(SummarizedStatus.SUMMARIZED);
        if (summarizedNewsList.isEmpty()) {
            throw new RuntimeException("No summarized news to generate tweets from.");
        }
        for (SummarizedNews summarizedNews : summarizedNewsList) {
            String tweet = generateTweet(summarizedNews.getContent());
            Tweet tweetRecord = new Tweet();
            tweetRecord.setBot(summarizedNews.getBot());
            tweetRecord.setNews(summarizedNews.getNews());
            tweetRecord.setCreatedAt(Timestamp.valueOf(LocalDateTime.now()));

            if (!tweet.startsWith("Error") || !tweet.startsWith("429")) {
                tweetRecord.setContent(tweet);
                tweetRecord.setStatus(TweetStatus.GENERATED);
            } else {
                tweetRecord.setContent(null);
                tweetRecord.setStatus(TweetStatus.FAILED);
            }
            tweetsRepository.save(tweetRecord);
            Thread.sleep(300);
        }

        return tweetsRepository.findByStatus(TweetStatus.GENERATED);
    }
    @Override
    public void refreshAccessToken() {
        RestTemplate restTemplate = new RestTemplate();

        // Request body to refresh token
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("client_id", clientId);
        requestBody.put("client_secret", clientSecret);
        requestBody.put("refresh_token", geminiApiKey);
        requestBody.put("grant_type", "refresh_token");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(TOKEN_URL, HttpMethod.POST, entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                geminiApiKey = (String) response.getBody().get("access_token");
                System.out.println("Token successfully refreshed");
            }
        } catch (Exception e) {
            logger.error("Error refreshing access token: {}", e.getMessage());
        }
    }

    @Override
    @Scheduled(cron = "0 0 0 1/30 * *")
    public void checkAndRefreshToken() {
        if (ChronoUnit.DAYS.between(startTime, LocalDateTime.now()) >= 30) {
            refreshAccessToken();
            startTime = LocalDateTime.now();
        }
    }
}