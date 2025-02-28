package com.egemen.TweetBotTelegram.service.Impl;

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
import com.egemen.TweetBotTelegram.service.GeminiService;
import com.egemen.TweetBotTelegram.service.NewsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of NewsService responsible for fetching and processing news.
 */
@Service
@RequiredArgsConstructor
public class NewsServiceImpl implements NewsService {

    @Value("${mediastack.api.key}")
    private String mediaStackApiKey;

    private final String mediaStackApiUrl = "http://api.mediastack.com/v1/news";

    private static final Logger log = LogManager.getLogger(NewsServiceImpl.class);

    @Autowired
    private NewsRepository newsRepository;

    @Autowired
    private BotRepository botsRepository;

    @Autowired
    private FetchLogsRepository fetchLogsRepository;

    @Autowired
    private BotConfigRepository botConfigurationRepository;


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
                    "&languages=en" +
                    "&limit=" + limit;
        } else {
            url = mediaStackApiUrl + "?access_key=" + mediaStackApiKey +
                    "&categories=" + topic +
                    "&countries=tr" +
                    "&languages=en" +
                    "&limit=" + limit;
        }

        log.info("Fetching news for bot {} with topic {} and limit {}", botId, topic, limit);
        log.debug("Request URL: {}", url);

        int retryCount = 0;
        String botRetryCount = (getBotConfig(bot, ConfigType.MAX_RETRIES));
        int maxRetries = botRetryCount == null ? 3 : Integer.parseInt(botRetryCount);
        int waitTime = 5000; // Başlangıç bekleme süresi 5 san,ye

        while (retryCount <= maxRetries) {
            try {
                RestTemplate restTemplate = new RestTemplate();
                ResponseEntity<NewsResponseDTO> response = restTemplate.getForEntity(url, NewsResponseDTO.class);

                if (response.getBody() == null || response.getBody().getArticles() == null) {
                    log.warn("No news articles found in API response for bot: {}", botId);
                    return Collections.emptyList();
                }

                List<News> articles = response.getBody().getArticles().stream()
                        .filter(article -> article.getTitle() != null && !article.getTitle().isEmpty())
                        .map(article -> {
                            Timestamp publishedAt = new Timestamp(System.currentTimeMillis());
                            if (article.getPublishedAt() != null) {
                                try {
                                    publishedAt = Timestamp.from(article.getPublishedAt().toInstant());
                                } catch (Exception e) {
                                    log.warn("Failed to parse published date for article: {}, using current timestamp.",
                                            article.getTitle(), e);
                                }
                            }
                            return new News(
                                    bot,
                                    StringEscapeUtils.unescapeHtml4(article.getTitle()),
                                    article.getDescription() != null ? StringEscapeUtils.unescapeHtml4(article.getDescription()) : "",
                                    publishedAt,
                                    NewsStatus.NOT_SUMMARIZED
                            );
                        })
                        .collect(Collectors.toList());
                FetchLogs fetchLog = new FetchLogs(bot, new Timestamp(System.currentTimeMillis()), FetchStatus.SUCCESS, articles.size());
                fetchLogsRepository.save(fetchLog);

                log.info("Successfully fetched {} articles for bot {}. Saving to database.", articles.size(), botId);
                return newsRepository.saveAll(articles);

            } catch (Exception e) {
                log.error("Error fetching news for bot {}: {}", botId, e.getMessage(), e);
                FetchLogs fetchLog = new FetchLogs(bot, new Timestamp(System.currentTimeMillis()), FetchStatus.FAILED, 0);
                fetchLogsRepository.save(fetchLog);

                // Retry logic
                retryCount++;
                if (retryCount > maxRetries) {
                    log.error("Max retries reached for bot {}. Giving up.", botId);
                    throw new RuntimeException("Failed to fetch news after " + maxRetries + " retries", e);
                }

                // Exponential backoff: Bekleme süresi her denemede artar
                int currentWaitTime = waitTime * (int) Math.pow(2, retryCount - 1);  // 5, 10, 20
                log.info("Retrying... Attempt {}/{}. Waiting for {} seconds...", retryCount, maxRetries, currentWaitTime / 1000);
                try {
                    Thread.sleep(currentWaitTime);  // İlgili süre kadar bekle
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
                .orElseThrow(() -> new RuntimeException(
                        String.format("Configuration %s not found for bot %d", configType, bot)));
    }
}