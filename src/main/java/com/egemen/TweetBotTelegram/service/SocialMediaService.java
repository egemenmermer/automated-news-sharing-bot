package com.egemen.TweetBotTelegram.service;

import com.egemen.TweetBotTelegram.entity.News;
import com.egemen.TweetBotTelegram.entity.InstagramPost;
import com.egemen.TweetBotTelegram.enums.NewsStatus;
import com.egemen.TweetBotTelegram.service.Impl.NewsImageServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SocialMediaService {

    private final InstagramApiService instagramService;
    private final NewsImageServiceImpl newsImageService;
    
    public boolean postToAllPlatforms(News news) {
        boolean anySuccess = false;
        
        // Try posting to Instagram
        try {
            // Generate image with text overlay
            String imagePath = newsImageService.generateNewsImage(news.getTitle(), news.getDescription());
            if (imagePath == null) {
                log.error("Failed to generate image for Instagram post");
                return false;
            }
            
            InstagramPost instagramPost = new InstagramPost();
            instagramPost.setCaption(news.getTitle() + "\n\n" + news.getDescription());
            instagramPost.setImageUrl(imagePath);
            instagramPost.setNews(news);
            
            boolean instagramSuccess = instagramService.createPost(instagramPost);
            if (instagramSuccess) {
                log.info("Successfully posted to Instagram: {}", news.getTitle());
                news.setStatus(NewsStatus.POSTED);
                anySuccess = true;
            } else {
                log.error("Failed to post to Instagram");
            }
        } catch (Exception e) {
            log.error("Error posting to Instagram", e);
        }
        
        return anySuccess;
    }
}