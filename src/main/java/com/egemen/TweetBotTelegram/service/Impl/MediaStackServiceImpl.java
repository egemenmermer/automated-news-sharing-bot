package com.egemen.TweetBotTelegram.service.Impl;

import com.egemen.TweetBotTelegram.entity.News;
import com.egemen.TweetBotTelegram.service.MediaStackService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class MediaStackServiceImpl implements MediaStackService {

    private final String apiKey;
    private final RestTemplate restTemplate;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

    @Autowired
    public MediaStackServiceImpl(String mediaStackApiKey) {
        this.apiKey = mediaStackApiKey;
        this.restTemplate = new RestTemplate();
        log.info("MediaStackServiceImpl initialized with API key");
    }

    @Override
    public List<News> fetchNews(String category, int limit) {
        try {
            String url = "http://api.mediastack.com/v1/news";
            
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url)
                    .queryParam("access_key", apiKey)
                    .queryParam("categories", category)
                    .queryParam("limit", limit)
                    .queryParam("languages", "en");
            
            Map<String, Object> response = restTemplate.getForObject(builder.toUriString(), Map.class);
            
            List<News> newsList = new ArrayList<>();
            
            if (response != null && response.containsKey("data")) {
                List<Map<String, Object>> data = (List<Map<String, Object>>) response.get("data");
                
                for (Map<String, Object> item : data) {
                    News news = new News();
                    news.setTitle((String) item.get("title"));
                    news.setContent((String) item.get("description"));
                    news.setUrl((String) item.get("url"));
                    news.setImageUrl((String) item.get("image"));
                    news.setSource((String) item.get("source"));
                    news.setCategory((String) item.get("category"));
                    
                    // Convert the string date to a Timestamp
                    String publishedAtStr = (String) item.get("published_at");
                    if (publishedAtStr != null && !publishedAtStr.isEmpty()) {
                        try {
                            java.util.Date parsedDate = dateFormat.parse(publishedAtStr);
                            Timestamp timestamp = new Timestamp(parsedDate.getTime());
                            news.setPublishedAt(timestamp);
                        } catch (Exception e) {
                            log.error("Error parsing date: {}", e.getMessage());
                            news.setPublishedAt(new Timestamp(System.currentTimeMillis()));
                        }
                    } else {
                        news.setPublishedAt(new Timestamp(System.currentTimeMillis()));
                    }
                    
                    newsList.add(news);
                }
            }
            
            return newsList;
        } catch (Exception e) {
            log.error("Error fetching news: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public List<News> fetchNews() {
        // Default categories and limit
        return fetchNews("general,business,technology", 10);
    }
} 