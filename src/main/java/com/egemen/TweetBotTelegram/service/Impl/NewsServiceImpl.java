package com.egemen.TweetBotTelegram.service.Impl;

import com.egemen.TweetBotTelegram.dto.NewsArticleDTO;
import com.egemen.TweetBotTelegram.dto.NewsResponseDTO;
import com.egemen.TweetBotTelegram.entity.News;
import com.egemen.TweetBotTelegram.repository.NewsRepository;
import com.egemen.TweetBotTelegram.service.NewsService;
import com.egemen.TweetBotTelegram.service.GeminiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class NewsServiceImpl implements NewsService {
    private static final Logger log = LoggerFactory.getLogger(NewsServiceImpl.class);
    
    private final RestTemplate restTemplate;
    private final String mediaStackApiKey;
    private final NewsRepository newsRepository;
    private final GeminiService geminiService;

    public NewsServiceImpl(
            RestTemplate restTemplate,
            @Value("${mediastack.api.key}") String mediaStackApiKey,
            NewsRepository newsRepository,
            GeminiService geminiService) {
        this.restTemplate = restTemplate;
        this.mediaStackApiKey = mediaStackApiKey;
        this.newsRepository = newsRepository;
        this.geminiService = geminiService;
    }

    @Override
    public List<News> getAllNews() {
        return newsRepository.findAll();
    }

    @Override
    public List<News> getUnprocessedNews() {
        return newsRepository.findByProcessedFalseAndPostedFalseOrderByCreatedAtDesc();
    }

    @Override
    public News getNews(Long id) {
        return newsRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("News not found with id: " + id));
    }

    @Override
    public List<News> fetchLatestNews() {
        String url = String.format("http://api.mediastack.com/v1/news?access_key=%s&languages=en&limit=10", mediaStackApiKey);
        
        NewsResponseDTO response = restTemplate.getForObject(url, NewsResponseDTO.class);
        List<News> newsList = new ArrayList<>();
        
        if (response != null && response.getArticles() != null) {
            for (NewsArticleDTO article : response.getArticles()) {
                News news = new News();
                news.setTitle(article.getTitle());
                news.setDescription(article.getDescription());
                news.setUrl(article.getUrl());
                
                // Generate summary using Gemini
                String summary = geminiService.generateSummary(article.getTitle(), article.getDescription());
                news.setSummary(summary);
                
                news.setProcessed(false);
                news.setPosted(false);
                news.setCreatedAt(LocalDateTime.now());
                
                newsList.add(newsRepository.save(news));
            }
        }
        
        return newsList;
    }

    @Override
    public void deleteNews(Long id) {
        newsRepository.deleteById(id);
    }

    @Override
    public News save(News news) {
        return newsRepository.save(news);
    }

    @Override
    public News processNewsImage(News news) {
        if (news.getImageUrl() != null && !news.isProcessed()) {
            try {
                // Mark as processed
                news.setProcessed(true);
                return newsRepository.save(news);
            } catch (Exception e) {
                log.error("Error processing image for news {}: {}", news.getId(), e.getMessage());
            }
        }
        return news;
    }

    @Override
    public void markAsPosted(News news) {
        news.setPosted(true);
        newsRepository.save(news);
    }

    private boolean newsExists(News news) {
        return newsRepository.existsByTitleAndDescription(news.getTitle(), news.getDescription());
    }

    private News convertToNews(NewsArticleDTO article) {
        if (article == null || article.getTitle() == null) {
            throw new IllegalArgumentException("Invalid article data");
        }

        News news = new News();
        news.setTitle(article.getTitle());
        news.setDescription(article.getDescription());
        news.setImageUrl(article.getImageUrl());
        news.setCreatedAt(LocalDateTime.now());
        news.setProcessed(false);
        news.setPosted(false);
        
        return news;
    }
}