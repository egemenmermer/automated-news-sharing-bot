package com.egemen.TweetBotTelegram.service.Impl;

import com.egemen.TweetBotTelegram.entity.News;
import com.egemen.TweetBotTelegram.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class NewsProcessorImpl implements NewsProcessor {
    private final NewsService newsService;
    private final GeminiService geminiService;
    private final ImageGenerationService imageGenerationService;
    private final ImageService imageService;
    private final InstagramService instagramService;
    private final S3Service s3Service;
    private final RateLimiterService rateLimiterService;

    @Override
    @Scheduled(fixedRateString = "${app.scheduler.news-fetch-rate:300000}")
    public void processNews() {
        try {
            // Rate limiting
            rateLimiterService.checkRateLimit("news_processing", 10, 3600);
            
            // 1. Fetch unprocessed news
            List<News> unprocessedNews = newsService.getUnprocessedNews();
            
            for (News news : unprocessedNews) {
                try {
                    // 2. Generate summary using Gemini
                    String summary = geminiService.summarizeNews(news.getTitle(), news.getContent());
                    
                    // 3. Generate image prompt
                    String imagePrompt = geminiService.generateImagePrompt(news.getTitle(), summary);
                    
                    // 4. Generate image
                    String generatedImagePath = imageGenerationService.generateNewsImage(news.getTitle(), summary);
                    
                    // 5. Process image for Instagram
                    String processedImageUrl = imageService.processImageForInstagram(generatedImagePath);
                    
                    // 6. Upload to S3
                    try (InputStream imageStream = new FileInputStream(new File(processedImageUrl))) {
                        String s3Url = s3Service.uploadImage(imageStream, 
                            "news-" + news.getId() + ".png");
                        
                        // 7. Generate Instagram caption
                        String caption = geminiService.generateCaption(news.getTitle(), summary);
                        
                        // 8. Create Instagram post
                        instagramService.createPost(news.getTitle(), caption, s3Url);
                        
                        // 9. Update news as processed
                        news.setProcessed(true);
                        news.setImageUrl(s3Url);
                        newsService.updateNews(news);
                    }
                } catch (Exception e) {
                    log.error("Error processing news item: {}", news.getTitle(), e);
                }
            }
        } catch (Exception e) {
            log.error("Error in news processing cycle", e);
        }
    }

    @Override
    @Scheduled(fixedRateString = "${app.scheduler.post-publish-rate:3600000}")
    public void publishPendingPosts() {
        try {
            rateLimiterService.checkRateLimit("instagram_posting", 5, 3600);
            instagramService.publishPendingPosts();
        } catch (Exception e) {
            log.error("Error publishing pending posts", e);
        }
    }
} 