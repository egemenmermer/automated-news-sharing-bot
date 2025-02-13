package com.egemen.TweetBotTelegram.scheduler;

import com.egemen.TweetBotTelegram.service.NewsService;
import com.egemen.TweetBotTelegram.service.ImageService;
import com.egemen.TweetBotTelegram.service.InstagramService;
import com.egemen.TweetBotTelegram.service.BotService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.List;
import com.egemen.TweetBotTelegram.enums.PostStatus;
import com.egemen.TweetBotTelegram.entity.InstagramPost;
import com.egemen.TweetBotTelegram.repository.InstagramPostRepository;
import lombok.extern.slf4j.Slf4j;

@Component
public class SocialMediaScheduler {
    private static final Logger log = LoggerFactory.getLogger(SocialMediaScheduler.class);
    
    private final NewsService newsService;
    private final ImageService imageService;
    private final InstagramService instagramService;
    private final BotService botService;
    private final InstagramPostRepository instagramPostRepository;
    private final int maxRetries;

    @Value("${app.social-media.twitter.batch-size:10}")
    private int twitterBatchSize;

    @Value("${app.social-media.telegram.batch-size:5}")
    private int telegramBatchSize;

    public SocialMediaScheduler(
            NewsService newsService,
            ImageService imageService,
            InstagramService instagramService,
            BotService botService,
            InstagramPostRepository instagramPostRepository,
            @Value("${app.max-retries:3}") int maxRetries) {
        this.newsService = newsService;
        this.imageService = imageService;
        this.instagramService = instagramService;
        this.botService = botService;
        this.instagramPostRepository = instagramPostRepository;
        this.maxRetries = maxRetries;
    }

    @Scheduled(fixedRateString = "${app.scheduler.fetch-news-rate:300000}")
    public void fetchNews() {
        log.info("Fetching news...");
        newsService.fetchLatestNews();
    }

    @Scheduled(fixedRateString = "${app.scheduler.post-rate:600000}")
    public void postToSocialMedia() {
        log.info("Posting to social media...");
        instagramService.publishPendingPosts();
    }

    @Scheduled(fixedDelayString = "${app.scheduler.retry-delay:300000}")
    public void retryFailedPosts() {
        List<InstagramPost> failedPosts = instagramPostRepository.findRetryablePosts(PostStatus.FAILED, maxRetries);
        
    }
}