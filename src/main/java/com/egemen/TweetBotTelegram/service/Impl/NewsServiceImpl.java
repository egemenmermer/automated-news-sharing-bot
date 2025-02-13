package com.egemen.TweetBotTelegram.service.impl;

import com.egemen.TweetBotTelegram.dto.NewsArticleDTO;
import com.egemen.TweetBotTelegram.dto.NewsResponseDTO;
import com.egemen.TweetBotTelegram.entity.News;
import com.egemen.TweetBotTelegram.repository.NewsRepository;
import com.egemen.TweetBotTelegram.service.NewsService;
import com.egemen.TweetBotTelegram.service.GeminiService;
import com.egemen.TweetBotTelegram.service.TelegramService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class NewsServiceImpl implements NewsService {
    private final RestTemplate restTemplate;
    private final String mediaStackApiKey;
    private final NewsRepository newsRepository;
    private final GeminiService geminiService;
    private final TelegramService telegramService;

    public NewsServiceImpl(
            RestTemplate restTemplate,
            @Value("${mediastack.api.key}") String mediaStackApiKey,
            NewsRepository newsRepository,
            GeminiService geminiService,
            TelegramService telegramService) {
        this.restTemplate = restTemplate;
        this.mediaStackApiKey = mediaStackApiKey;
        this.newsRepository = newsRepository;
        this.geminiService = geminiService;
        this.telegramService = telegramService;
    }

    @Override
    public List<News> getAllNews() {
        return newsRepository.findAll();
    }

    @Override
    public List<News> getUnprocessedNews() {
        return newsRepository.findByProcessedFalse();
    }

    @Override
    public News getNews(Long id) {
        return newsRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("News not found with id: " + id));
    }

    @Override
    public void fetchLatestNews() {
        try {
            String url = buildMediaStackUrl();
            NewsResponseDTO response = restTemplate.getForObject(url, NewsResponseDTO.class);
            
            if (response != null && response.getData() != null) {
                List<News> newsList = new ArrayList<>();
                for (NewsArticleDTO article : response.getData()) {
                    News news = convertToNews(article);
                    newsList.add(news);
                }
                newsRepository.saveAll(newsList);
                log.info("Saved {} news articles", newsList.size());
            }
        } catch (Exception e) {
            log.error("Error fetching news: {}", e.getMessage());
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

    @Override
    public void processNews(News news) {
        try {
            String summary = geminiService.summarizeNews(news.getTitle(), news.getContent());
            
            telegramService.sendNewsUpdate(
                "YOUR_CHAT_ID", // Replace with actual chat ID
                news.getTitle(),
                summary,
                news.getImageUrl()
            );
            
        } catch (Exception e) {
            log.error("Error processing news: {}", e.getMessage(), e);
            telegramService.sendError("YOUR_CHAT_ID", "Error processing news: " + e.getMessage());
        }
    }

    private boolean newsExists(News news) {
        return newsRepository.existsByTitleAndDescription(news.getTitle(), news.getDescription());
    }

    private String buildMediaStackUrl() {
        return String.format("http://api.mediastack.com/v1/news?access_key=%s&languages=en&limit=10", mediaStackApiKey);
    }

    private News convertToNews(NewsArticleDTO article) {
        News news = new News();
        news.setTitle(article.getTitle());
        news.setContent(article.getDescription());
        news.setPublishedAt(LocalDateTime.now()); // or parse from article if available
        news.setProcessed(false);
        return news;
    }
}