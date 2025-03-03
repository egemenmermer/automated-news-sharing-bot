package com.egemen.TweetBotTelegram.service;

import com.egemen.TweetBotTelegram.entity.Bot;
import com.egemen.TweetBotTelegram.entity.InstagramPost;
import com.egemen.TweetBotTelegram.entity.News;
import com.egemen.TweetBotTelegram.enums.PostStatus;
import com.egemen.TweetBotTelegram.repository.InstagramPostRepository;
import com.egemen.TweetBotTelegram.service.Impl.InstagramApiServiceImpl;
import com.egemen.TweetBotTelegram.service.Impl.NewsImageServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;

@Slf4j
@Service
public class SocialMediaService {

    private final InstagramApiServiceImpl instagramApiService;
    private final InstagramPostRepository instagramPostRepository;
    private final NewsImageServiceImpl newsImageService;

    public SocialMediaService(InstagramApiServiceImpl instagramApiService, InstagramPostRepository instagramPostRepository, NewsImageServiceImpl newsImageService) {
        this.instagramApiService = instagramApiService;
        this.instagramPostRepository = instagramPostRepository;
        this.newsImageService = newsImageService;
    }

    public boolean postToInstagram(News news, Bot bot, String caption, String imagePrompt) {
        try {
            // Generate image for the news article
            String imageUrl = newsImageService.generateNewsImage(imagePrompt, news.getTitle());
            
            // Create Instagram post entity
            InstagramPost instagramPost = new InstagramPost();
            instagramPost.setBot(bot);
            instagramPost.setNews(news);
            instagramPost.setCaption(caption);
            instagramPost.setImageUrl(imageUrl);
            instagramPost.setImagePrompt(imagePrompt);
            instagramPost.setPostStatus(PostStatus.PENDING);
            instagramPost.setCreatedAt(Timestamp.valueOf(LocalDateTime.now()));
            
            // Save the post to the database
            instagramPost = instagramPostRepository.save(instagramPost);
            
            // Post to Instagram
            boolean success = instagramApiService.createPost(instagramPost);
            
            return success;
        } catch (Exception e) {
            log.error("Error posting to Instagram: {}", e.getMessage());
            return false;
        }
    }
}