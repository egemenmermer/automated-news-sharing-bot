package com.egemen.TweetBotTelegram.service.Impl;

import com.egemen.TweetBotTelegram.entity.News;
import com.egemen.TweetBotTelegram.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class NewsProcessorImpl implements NewsProcessor {
    private final NewsService newsService;
    private final ImageService imageService;
    private final InstagramService instagramService;
    private final GeminiService geminiService;

    public NewsProcessorImpl(
            NewsService newsService,
            ImageService imageService,
            InstagramService instagramService,
            GeminiService geminiService) {
        this.newsService = newsService;
        this.imageService = imageService;
        this.instagramService = instagramService;
        this.geminiService = geminiService;
    }

    @Override
    @Scheduled(fixedRateString = "${app.scheduler.news-fetch-rate:300000}")
    public void processNews() {
        // ... same implementation as before ...
    }

    @Override
    @Scheduled(fixedRateString = "${app.scheduler.post-publish-rate:3600000}")
    public void publishPendingPosts() {
        // ... same implementation as before ...
    }
} 