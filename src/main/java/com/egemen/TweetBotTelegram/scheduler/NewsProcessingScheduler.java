package com.egemen.TweetBotTelegram.scheduler;

import com.egemen.TweetBotTelegram.entity.InstagramPost;
import com.egemen.TweetBotTelegram.entity.News;
import com.egemen.TweetBotTelegram.enums.NewsStatus;
import com.egemen.TweetBotTelegram.enums.PostStatus;
import com.egemen.TweetBotTelegram.repository.NewsRepository;
import com.egemen.TweetBotTelegram.service.GeminiService;
import com.egemen.TweetBotTelegram.service.InstagramApiService;
import com.egemen.TweetBotTelegram.service.PexelsService;
import com.egemen.TweetBotTelegram.service.Impl.ImageSaver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/**
 * Created NewsProcessingScheduler to handle automatic news processing and Instagram posting
 */
@Slf4j
@Component
public class NewsProcessingScheduler {

    @Value("${news.processing.batch-size:5}")
    private int batchSize;

    private final NewsRepository newsRepository;
    private final GeminiService geminiService;
    private final InstagramApiService instagramApiService;
    private final ImageSaver imageSaver;
    private final PexelsService pexelsService;

    public NewsProcessingScheduler(
            NewsRepository newsRepository,
            GeminiService geminiService,
            InstagramApiService instagramApiService,
            ImageSaver imageSaver,
            PexelsService pexelsService) {
        this.newsRepository = newsRepository;
        this.geminiService = geminiService;
        this.instagramApiService = instagramApiService;
        this.imageSaver = imageSaver;
        this.pexelsService = pexelsService;
    }

    private boolean isSchedulerEnabled = false;

    public void enableScheduler() {
        isSchedulerEnabled = true;
        processUnpostedNews();
    }

    public void disableScheduler() {
        isSchedulerEnabled = false;
    }

    public boolean isSchedulerEnabled() {
        return isSchedulerEnabled;
    }

    @Scheduled(fixedDelayString = "${news.processing.interval:300000}")
    public void processUnpostedNews() {
        if (!isSchedulerEnabled) {
            log.info("News processing scheduler is disabled");
            return;
        }
        log.info("Starting news processing scheduler...");
        
        try {
            // Process news in smaller batches with individual transactions
            List<News> unpostedNews = newsRepository.findByStatus(NewsStatus.PENDING, PageRequest.of(0, batchSize));
            log.info("Found {} unposted news articles", unpostedNews.size());
            
            for (News news : unpostedNews) {
                processNewsItemWithTransaction(news);
            }
        } catch (Exception e) {
            log.error("Error in news processing scheduler: {}", e.getMessage(), e);
        }
    }
    
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processNewsItemWithTransaction(News news) {
        try {
            processNewsItem(news);
        } catch (Exception e) {
            log.error("Error processing news item {}: {}", news.getId(), e.getMessage());
            news.setStatus(NewsStatus.FAILED);
            newsRepository.save(news);
        }
    }

    private void processNewsItem(News news) throws Exception {
        log.info("Processing news item: {}", news.getId());

        // Generate image prompt using Gemini
        String imagePrompt = geminiService.generateImagePrompt(news.getTitle(), news.getContent());
        log.info("Generated image prompt: {}", imagePrompt);

        // Generate summary for Instagram caption
        String summary = geminiService.generateSummary(news.getTitle(), news.getContent());
        log.info("Generated summary: {}", summary);

        try {
            // Generate image using Pexels API
            byte[] imageBytes = pexelsService.searchAndFetchImage(imagePrompt);
            if (imageBytes == null || imageBytes.length == 0) {
                log.error("Failed to generate image for news {}", news.getId());
                news.setStatus(NewsStatus.FAILED);
                newsRepository.save(news);
                return;
            }

            // Save image and get URL
            String imageUrl = imageSaver.saveImageToFile(imageBytes);
            log.info("Saved image to: {}", imageUrl);

            // Create Instagram post
            InstagramPost post = new InstagramPost();
            post.setNews(news);
            post.setCaption(formatCaption(summary, news.getSource()));
            post.setImagePrompt(imagePrompt);
            post.setImageUrl(imageUrl);
            post.setPostStatus(PostStatus.PENDING);
            post.setCreatedAt(Timestamp.valueOf(LocalDateTime.now()));

            // Try to post to Instagram
            boolean posted = instagramApiService.createPost(post);

            if (posted) {
                news.setStatus(NewsStatus.POSTED);
                log.info("Successfully posted news {} to Instagram", news.getId());
            } else {
                news.setStatus(NewsStatus.FAILED);
                log.error("Failed to post news {} to Instagram", news.getId());
            }

            newsRepository.save(news);
        } catch (Exception e) {
            log.error("Error processing news item: {}", e.getMessage());
            news.setStatus(NewsStatus.FAILED);
            newsRepository.save(news);
            throw e;
        }
    }

    private String formatCaption(String summary, String source) {
        return String.format("%s\n\nSource: %s\n\n#news #ai #neuralNews", summary, source);
    }
}
