package com.egemen.TweetBotTelegram.service.Impl;

import com.egemen.TweetBotTelegram.dto.ArticleDTO;
import com.egemen.TweetBotTelegram.dto.NewsResponseDTO;
import com.egemen.TweetBotTelegram.entity.Bot;
import com.egemen.TweetBotTelegram.entity.FetchLogs;
import com.egemen.TweetBotTelegram.entity.News;
import com.egemen.TweetBotTelegram.enums.ConfigType;
import com.egemen.TweetBotTelegram.enums.FetchStatus;
import com.egemen.TweetBotTelegram.enums.NewsStatus;
import com.egemen.TweetBotTelegram.repository.BotConfigRepository;
import com.egemen.TweetBotTelegram.repository.BotRepository;
import com.egemen.TweetBotTelegram.repository.FetchLogsRepository;
import com.egemen.TweetBotTelegram.repository.NewsRepository;
import com.egemen.TweetBotTelegram.service.MediaStackService;
import com.egemen.TweetBotTelegram.service.NewsService;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringEscapeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class NewsServiceImpl implements NewsService {

    private final String mediaStackApiKey;
    private final NewsRepository newsRepository;
    private final BotRepository botsRepository;
    private final FetchLogsRepository fetchLogsRepository;
    private final BotConfigRepository botConfigurationRepository;
    private final MediaStackService mediaStackService;
    private static final String mediaStackApiUrl = "http://api.mediastack.com/v1/news";

    @Autowired
    public NewsServiceImpl(String mediaStackApiKey,
                         NewsRepository newsRepository,
                         BotRepository botsRepository,
                         FetchLogsRepository fetchLogsRepository,
                         BotConfigRepository botConfigurationRepository,
                         MediaStackService mediaStackService) {
        this.mediaStackApiKey = mediaStackApiKey;
        this.newsRepository = newsRepository;
        this.botsRepository = botsRepository;
        this.fetchLogsRepository = fetchLogsRepository;
        this.botConfigurationRepository = botConfigurationRepository;
        this.mediaStackService = mediaStackService;
        log.info("NewsServiceImpl initialized with MediaStack API key");
    }

    @Override
    public List<News> fetchAndSaveNews(Long botId, boolean isTR) {
        Bot bot = botsRepository.findById(botId)
                .orElseThrow(() -> new RuntimeException("Bot not found with id: " + botId));

        // Fetch bot configurations
        String topic = getBotConfig(bot, ConfigType.TOPIC).toLowerCase();
        String fetchAmount = getBotConfig(bot, ConfigType.FETCH_AMOUNT);
        int limit = fetchAmount != null ? Integer.parseInt(fetchAmount) : 10;
        String url;
        if (isTR) {
            url = mediaStackApiUrl + "?access_key=" + mediaStackApiKey +
                    "&categories=" + topic +
                    "&languages=tr" +
                    "&limit=" + limit;
        } else {
            url = mediaStackApiUrl + "?access_key=" + mediaStackApiKey +
                    "&categories=" + topic +
                    "&languages=en" +
                    "&limit=" + limit;
        }

        log.info("Fetching news for bot {} with topic {} and limit {}", botId, topic, limit);
        log.debug("Request URL: {}", url);

        int retryCount = 0;
        String botRetryCount = (getBotConfig(bot, ConfigType.MAX_RETRIES));
        int maxRetries = botRetryCount == null ? 3 : Integer.parseInt(botRetryCount);
        int waitTime = 5000;

        while (retryCount <= maxRetries) {
            try {
                RestTemplate restTemplate = new RestTemplate();
                restTemplate.setErrorHandler(new ResponseErrorHandler() {
                    @Override
                    public boolean hasError(ClientHttpResponse response) throws IOException {
                        return false;
                    }

                    @Override
                    public void handleError(ClientHttpResponse response) throws IOException {
                    }
                });

                log.info("Making request to URL: {}", url);
                ResponseEntity<String> rawResponse = restTemplate.getForEntity(url, String.class);
                log.info("Raw response: {}", rawResponse.getBody());

                ObjectMapper objectMapper = new ObjectMapper();
                objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                objectMapper.registerModule(new JavaTimeModule());

                JsonNode rootNode = objectMapper.readTree(rawResponse.getBody());
                JsonNode dataNode = rootNode.get("data");

                if (dataNode == null || !dataNode.isArray() || dataNode.size() == 0) {
                    log.warn("No news data found in response");
                    return Collections.emptyList();
                }

                List<News> articles = new ArrayList<>();
                for (JsonNode articleNode : dataNode) {
                    ArticleDTO article = objectMapper.treeToValue(articleNode, ArticleDTO.class);
                    
                    // Check if article already exists in database
                    if (newsRepository.existsByTitleAndBot(article.getTitle(), bot)) {
                        log.info("Article already exists: {}", article.getTitle());
                        continue;
                    }

                    // Parse published_at timestamp
                    Timestamp publishedAt = null;
                    if (article.getPublishedAt() != null) {
                        try {
                            // Convert OffsetDateTime to Timestamp
                            publishedAt = Timestamp.from(article.getPublishedAt().toInstant());
                        } catch (Exception e) {
                            log.error("Error parsing published_at: {}", e.getMessage());
                            publishedAt = new Timestamp(System.currentTimeMillis());
                        }
                    } else {
                        publishedAt = new Timestamp(System.currentTimeMillis());
                    }

                    // Create a new News object using setters
                    News news = new News();
                    news.setBot(bot);
                    news.setTitle(StringEscapeUtils.unescapeHtml4(article.getTitle()));
                    news.setContent(article.getDescription() != null ? 
                                   StringEscapeUtils.unescapeHtml4(article.getDescription()) : "");
                    news.setSource(article.getSource() != null ? article.getSource() : "Unknown");
                    news.setUrl(article.getUrl());
                    news.setImageUrl(article.getImage());
                    news.setCategory(article.getCategory());
                    news.setPublishedAt(publishedAt);
                    news.setStatus(NewsStatus.PENDING);

                    articles.add(news);
                    log.info("Added article: {}", article.getTitle());
                }

                // Save all articles to database
                List<News> savedArticles = newsRepository.saveAll(articles);

                // Log the fetch
                FetchLogs fetchLog = new FetchLogs();
                fetchLog.setBot(bot);
                fetchLog.setFetchTime(new Timestamp(System.currentTimeMillis()));
                fetchLog.setArticleCount(savedArticles.size());
                fetchLog.setStatus(FetchStatus.SUCCESS);
                fetchLogsRepository.save(fetchLog);

                log.info("Successfully fetched and saved {} articles", savedArticles.size());
                return savedArticles;
            } catch (Exception e) {
                log.error("Error fetching news: {}", e.getMessage());
                retryCount++;
                if (retryCount <= maxRetries) {
                    log.info("Retrying in {} ms (attempt {}/{})", waitTime, retryCount, maxRetries);
                    try {
                        Thread.sleep(waitTime);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                    waitTime *= 2; // Exponential backoff
                } else {
                    log.error("Max retries reached, giving up");
                    
                    // Log the failed fetch
                    FetchLogs fetchLog = new FetchLogs();
                    fetchLog.setBot(bot);
                    fetchLog.setFetchTime(new Timestamp(System.currentTimeMillis()));
                    fetchLog.setArticleCount(0);
                    fetchLog.setStatus(FetchStatus.FAILED);
                    fetchLog.setErrorMessage(e.getMessage());
                    fetchLogsRepository.save(fetchLog);
                    
                    return Collections.emptyList();
                }
            }
        }
        
        return Collections.emptyList();
    }

    public String getBotConfig(Bot bot, ConfigType configType) {
        return botConfigurationRepository.findConfigValueByBotIdAndConfigType(bot, configType)
                .orElseGet(() -> {
                    log.warn("Configuration {} not found for bot {}, using default value", configType, bot.getId());
                    switch (configType) {
                        case TOPIC:
                            return "technology";
                        case FETCH_AMOUNT:
                            return "10";
                        case MAX_RETRIES:
                            return "3";
                        default:
                            return null;
                    }
                });
    }

    @Override
    public List<News> getAllNews() {
        return newsRepository.findAll();
    }

    @Override
    @Transactional
    public List<News> getPendingNews(Long botId) {
        Bot bot = botsRepository.findById(botId)
                .orElseThrow(() -> new RuntimeException("Bot not found with id: " + botId));
        return newsRepository.findByStatus(NewsStatus.PENDING);
    }

    @Override
    @Transactional
    public News saveNews(News news) {
        log.info("Saving news: {}", news.getTitle());
        return newsRepository.save(news);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<News> getNewsById(Long id) {
        log.info("Getting news with ID: {}", id);
        return newsRepository.findById(id);
    }

    @Override
    @Transactional
    public void deleteNews(Long id) {
        log.info("Deleting news with ID: {}", id);
        newsRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<News> getNewsByStatus(NewsStatus status) {
        log.info("Getting news with status: {}", status);
        return newsRepository.findByStatus(status);
    }

    @Override
    @Transactional(readOnly = true)
    public List<News> getNewsByBot(Bot bot) {
        log.info("Getting news for bot: {}", bot.getName());
        return newsRepository.findByBot(bot);
    }

    @Override
    @Transactional(readOnly = true)
    public List<News> getNewsByBotAndStatus(Bot bot, NewsStatus status) {
        log.info("Getting news for bot: {} with status: {}", bot.getName(), status);
        return newsRepository.findByBotAndStatus(bot, status);
    }

    @Override
    @Transactional
    public void updateNewsStatus(Long newsId, NewsStatus status) {
        News news = newsRepository.findById(newsId)
            .orElseThrow(() -> new RuntimeException("News not found with id: " + newsId));
        news.setStatus(status);
        newsRepository.save(news);
        log.info("Updated news status to {} for news id: {}", status, newsId);
    }

    @Override
    @Transactional
    public void markNewsAsPosted(Long id) {
        try {
            News news = newsRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("News not found with id: " + id));
            news.setStatus(NewsStatus.POSTED);
            newsRepository.save(news);
            log.info("Marked news {} as POSTED", id);
        } catch (Exception e) {
            log.error("Error marking news {} as posted: {}", id, e.getMessage(), e);
            throw new RuntimeException("Failed to mark news as posted", e);
        }
    }

    @Override
    @Transactional
    public void markNewsAsFailed(Long id) {
        try {
            News news = newsRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("News not found with id: " + id));
            news.setStatus(NewsStatus.FAILED);
            newsRepository.save(news);
            log.info("Marked news {} as FAILED", id);
        } catch (Exception e) {
            log.error("Error marking news {} as failed: {}", id, e.getMessage(), e);
            throw new RuntimeException("Failed to mark news as failed", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<News> getPendingNews(Bot bot, int limit) {
        return newsRepository.findByBotAndStatusOrderByPublishedAtDesc(bot, NewsStatus.PENDING, limit);
    }
}