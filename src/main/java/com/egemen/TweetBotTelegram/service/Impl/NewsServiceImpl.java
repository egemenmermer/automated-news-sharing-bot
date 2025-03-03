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
import com.egemen.TweetBotTelegram.service.NewsService;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringEscapeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
public class NewsServiceImpl implements NewsService {

    @Value("${MEDIASTACK_API_KEY}")
    private String mediaStackApiKey;
    private final NewsRepository newsRepository;
    private final BotRepository botsRepository;
    private final FetchLogsRepository fetchLogsRepository;
    private final BotConfigRepository botConfigurationRepository;
    private static final String mediaStackApiUrl = "http://api.mediastack.com/v1/news";

    @Autowired
    public NewsServiceImpl(NewsRepository newsRepository,
                         BotRepository botsRepository,
                         FetchLogsRepository fetchLogsRepository,
                         BotConfigRepository botConfigurationRepository) {
        this.newsRepository = newsRepository;
        this.botsRepository = botsRepository;
        this.fetchLogsRepository = fetchLogsRepository;
        this.botConfigurationRepository = botConfigurationRepository;
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

                ObjectMapper mapper = new ObjectMapper();
                mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                mapper.registerModule(new JavaTimeModule());
                
                JsonNode root = mapper.readTree(rawResponse.getBody());
                JsonNode dataNode = root.get("data");
                
                if (dataNode == null || !dataNode.isArray()) {
                    log.warn("No 'data' array found in response for bot: {}", botId);
                    return Collections.emptyList();
                }

                List<News> articles = new ArrayList<>();
                for (JsonNode articleNode : dataNode) {
                    try {
                        ArticleDTO article = mapper.treeToValue(articleNode, ArticleDTO.class);
                        if (article.getTitle() != null && !article.getTitle().isEmpty()) {
                            Timestamp publishedAt = new Timestamp(System.currentTimeMillis());
                            if (article.getPublishedAt() != null) {
                                try {
                                    publishedAt = Timestamp.from(article.getPublishedAt().toInstant());
                                } catch (Exception e) {
                                    log.warn("Failed to parse published date for article: {}, using current timestamp.",
                                            article.getTitle(), e);
                                }
                            }
                            News news = new News(
                                    bot,
                                    StringEscapeUtils.unescapeHtml4(article.getTitle()),
                                    article.getDescription() != null ? StringEscapeUtils.unescapeHtml4(article.getDescription()) : "",
                                    article.getSource() != null ? article.getSource() : "Unknown",
                                    publishedAt,
                                    NewsStatus.PENDING
                            );
                            articles.add(news);
                            log.info("Added article: {}", article.getTitle());
                        }
                    } catch (Exception e) {
                        log.error("Error processing article: {}", e.getMessage(), e);
                    }
                }

                if (!articles.isEmpty()) {
                    log.info("Saving {} articles to database", articles.size());
                    articles = newsRepository.saveAll(articles);
                    FetchLogs fetchLog = new FetchLogs(bot, new Timestamp(System.currentTimeMillis()), FetchStatus.SUCCESS, articles.size());
                    fetchLogsRepository.save(fetchLog);
                    return articles;
                } else {
                    log.warn("No valid articles found in response");
                    FetchLogs fetchLog = new FetchLogs(bot, new Timestamp(System.currentTimeMillis()), FetchStatus.SUCCESS, 0);
                    fetchLogsRepository.save(fetchLog);
                    return Collections.emptyList();
                }

            } catch (Exception e) {
                log.error("Error fetching news for bot {}: {}", botId, e.getMessage(), e);
                FetchLogs fetchLog = new FetchLogs(bot, new Timestamp(System.currentTimeMillis()), FetchStatus.FAILED, 0);
                fetchLogsRepository.save(fetchLog);

                retryCount++;
                if (retryCount > maxRetries) {
                    log.error("Max retries reached for bot {}. Giving up.", botId);
                    throw new RuntimeException("Failed to fetch news after " + maxRetries + " retries", e);
                }

                int currentWaitTime = waitTime * (int) Math.pow(2, retryCount - 1);
                log.info("Retrying... Attempt {}/{}. Waiting for {} seconds...", retryCount, maxRetries, currentWaitTime / 1000);
                try {
                    Thread.sleep(currentWaitTime);
                } catch (InterruptedException interruptedException) {
                    log.error("Retry sleep interrupted for bot {}", botId);
                    Thread.currentThread().interrupt();
                }
            }
        }

        throw new RuntimeException("Failed to fetch news after max retries");
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
    @org.springframework.transaction.annotation.Transactional
    public List<News> getPendingNews(Long botId) {
        Bot bot = botsRepository.findById(botId)
                .orElseThrow(() -> new RuntimeException("Bot not found with id: " + botId));
        return newsRepository.findByStatus(NewsStatus.PENDING);
    }
}