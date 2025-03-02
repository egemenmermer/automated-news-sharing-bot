package com.egemen.TweetBotTelegram.service;

import com.egemen.TweetBotTelegram.entity.News;
import com.egemen.TweetBotTelegram.entity.InstagramPost;
import com.egemen.TweetBotTelegram.enums.NewsStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SocialMediaService {

    private final InstagramApiService instagramService;
    
    public boolean postToAllPlatforms(News news) {
        boolean anySuccess = false;
        
        // Try posting to Instagram
        try {
            InstagramPost instagramPost = new InstagramPost();
            instagramPost.setCaption(news.getTitle() + "\n\n" + news.getDescription());
            instagramPost.setImageUrl(news.getGeneratedImagePath() != null ? news.getGeneratedImagePath() : news.getImageUrl());
            
            boolean instagramSuccess = instagramService.createPost(instagramPost);
            if (instagramSuccess) {
                log.info("Successfully posted to Instagram: {}", news.getTitle());
                news.setStatus(NewsStatus.POSTED);
                anySuccess = true;
            }
        } catch (Exception e) {
            log.error("Error posting to Instagram", e);
        }
        
        return anySuccess;
    }
}