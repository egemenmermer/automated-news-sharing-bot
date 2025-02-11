package com.egemen.TweetBotTelegram.service.Impl;

import com.egemen.TweetBotTelegram.dto.NewsArticleDTO;
import com.egemen.TweetBotTelegram.dto.NewsResponseDTO;
import com.egemen.TweetBotTelegram.entity.News;
import com.egemen.TweetBotTelegram.repository.NewsRepository;
import com.egemen.TweetBotTelegram.service.NewsService;
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

    @Value("${mediastack.api.key}")
    private String mediastackApiKey;

    private final NewsRepository newsRepository;
    private final RestTemplate restTemplate;
    private static final String NEWS_API_URL = "http://api.mediastack.com/v1/news";

    public NewsServiceImpl(NewsRepository newsRepository, RestTemplate restTemplate) {
        this.newsRepository = newsRepository;
        this.restTemplate = restTemplate;
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
        try {
            log.info("Fetching latest news from MediaStack API");
            
            String url = UriComponentsBuilder.fromHttpUrl(NEWS_API_URL)
                    .queryParam("access_key", mediastackApiKey)
                    .queryParam("languages", "en")
                    .queryParam("limit", 100)
                    .queryParam("sort", "published_desc")
                    .build()
                    .toUriString();

            ResponseEntity<NewsResponseDTO> response = restTemplate.getForEntity(url, NewsResponseDTO.class);
            List<News> savedNews = new ArrayList<>();

            if (response.getBody() != null && response.getBody().getArticles() != null) {
                for (NewsArticleDTO article : response.getBody().getArticles()) {
                    try {
                        News news = convertToNews(article);
                        if (!newsExists(news)) {
                            savedNews.add(newsRepository.save(news));
                            log.info("Saved new article: {}", news.getTitle());
                        }
                    } catch (Exception e) {
                        log.error("Error processing article: {}", e.getMessage());
                    }
                }
            }

            return savedNews;
        } catch (Exception e) {
            log.error("Error fetching news: {}", e.getMessage());
            throw new RuntimeException("Failed to fetch news", e);
        }
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