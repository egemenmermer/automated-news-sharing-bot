package com.egemen.TweetBotTelegram.service.Impl;

import com.egemen.TweetBotTelegram.entity.InstagramPost;
import com.egemen.TweetBotTelegram.enums.PostStatus;
import com.egemen.TweetBotTelegram.repository.InstagramPostRepository;
import com.egemen.TweetBotTelegram.service.InstagramApiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class InstagramApiServiceImpl implements InstagramApiService {

    @Value("${INSTAGRAM_ACCESS_TOKEN}")
    private String accessToken;

    @Value("${INSTAGRAM_USERID}")
    private String userId;

    private final RestTemplate restTemplate;
    private final InstagramPostRepository instagramPostRepository;

    public InstagramApiServiceImpl(InstagramPostRepository instagramPostRepository) {
        this.restTemplate = new RestTemplate();
        this.instagramPostRepository = instagramPostRepository;
    }

    @Override
    public boolean createPost(InstagramPost post) {
        try {
            log.info("Creating Instagram post with caption: {}", post.getCaption());
            
            // Check if Instagram API credentials are configured
            if (accessToken == null || accessToken.isEmpty() || userId == null || userId.isEmpty()) {
                String errorMessage = "Instagram API credentials not configured. Please check your environment variables.";
                log.error(errorMessage);
                post.setPostStatus(PostStatus.FAILED);
                post.setErrorMessage(errorMessage);
                post.setRetryCount(post.getRetryCount() + 1);
                instagramPostRepository.save(post);
                return false;
            }
            
            // Validate image URL
            if (post.getImageUrl() == null || post.getImageUrl().isEmpty()) {
                String errorMessage = "Image URL is required for Instagram post";
                log.error(errorMessage);
                post.setPostStatus(PostStatus.FAILED);
                post.setErrorMessage(errorMessage);
                post.setRetryCount(post.getRetryCount() + 1);
                instagramPostRepository.save(post);
                return false;
            }

            // Use the publishPost method to create and publish the post
            publishPost(post);
            return true;
        } catch (Exception e) {
            String errorMessage = String.format("Error creating Instagram post: %s", e.getMessage());
            log.error(errorMessage);
            post.setPostStatus(PostStatus.FAILED);
            post.setErrorMessage(errorMessage);
            post.setRetryCount(post.getRetryCount() + 1);
            instagramPostRepository.save(post);
            return false;
        }
    }

    @Override
    public String uploadImage(String imageUrl) throws Exception {
        try {
            String url = String.format("https://graph.instagram.com/v22.0/%s/media", userId);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(accessToken);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("image_url", imageUrl);
            requestBody.put("is_carousel_item", false);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, request, Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return (String) response.getBody().get("id");
            }
            
            throw new Exception("Failed to get media ID from response");
        } catch (Exception e) {
            log.error("Error uploading image to Instagram: {}", e.getMessage());
            throw e;
        }
    }

    @Override
    public void publishPost(InstagramPost post) throws Exception {
        try {
            // Get the media ID from uploadImage if not already done in createPost
            String mediaId = uploadImage(post.getImageUrl());
            String url = String.format("https://graph.instagram.com/v22.0/%s/media_publish", userId);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(accessToken);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("creation_id", mediaId);
            requestBody.put("caption", post.getCaption());
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, request, Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                String instagramPostId = (String) response.getBody().get("id");
                post.setInstagramPostId(instagramPostId);
                post.setPostStatus(PostStatus.PUBLISHED);
                post.setPostedAt(Timestamp.valueOf(LocalDateTime.now()));
                instagramPostRepository.save(post);
                log.info("Successfully published post to Instagram with ID: {}", instagramPostId);
            } else {
                throw new Exception("Failed to publish post to Instagram");
            }
        } catch (Exception e) {
            log.error("Error publishing post to Instagram: {}", e.getMessage());
            throw e;
        }
    }

    @Override
    public void deletePost(String mediaId) throws Exception {
        try {
            String url = String.format("https://graph.instagram.com/v22.0/%s", mediaId);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(accessToken);

            Map<String, Object> requestBody = new HashMap<>();
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.DELETE, request, Map.class);
            
            if (response.getStatusCode() != HttpStatus.OK) {
                throw new Exception("Failed to delete post from Instagram");
            }
            
            log.info("Successfully deleted post from Instagram");
        } catch (Exception e) {
            log.error("Error deleting post from Instagram: {}", e.getMessage());
            throw e;
        }
    }

    @Override
    public InstagramPost getPostById(Long id) {
        return instagramPostRepository.findById(id).orElse(null);
    }
}