package com.egemen.TweetBotTelegram.service.Impl;

import com.egemen.TweetBotTelegram.exception.InstagramApiException;
import com.egemen.TweetBotTelegram.service.InstagramApiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

@Service
public class InstagramApiServiceImpl implements InstagramApiService {

    private static final Logger logger = LoggerFactory.getLogger(InstagramApiServiceImpl.class);
    private static final int MAX_CAPTION_LENGTH = 2200;

    @Value("${instagram.access.token}")
    private String accessToken;

    @Value("${instagram.user.id}")
    private String instagramUserId;

    private final RestTemplate restTemplate = new RestTemplate();
    private static final String API_URL = "https://graph.instagram.com/v22.0";

    @Override
    @Retryable(value = InstagramApiException.class, maxAttempts = 3, backoff = @Backoff(delay = 2000))
    public String uploadImageToInstagram(String imageUrl, String caption) throws InstagramApiException {
        String url = API_URL + "/" + instagramUserId + "/media";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("image_url", imageUrl);
        requestBody.put("caption", caption);

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, requestEntity, Map.class);
            if (response.getBody() != null && response.getBody().containsKey("id")) {
                logger.info("Media container created: {}", response.getBody().get("id"));
                return (String) response.getBody().get("id");
            }
            throw new InstagramApiException("Failed to upload image");
        } catch (HttpClientErrorException e) {
            logger.error("Instagram API error: {}", e.getResponseBodyAsString());
            throw new InstagramApiException("Instagram API error: " + e.getMessage());
        }
    }

    @Override
    @Retryable(value = InstagramApiException.class, maxAttempts = 3, backoff = @Backoff(delay = 2000))
    public boolean publishPost(String mediaId) throws InstagramApiException {
        String url = API_URL + "/" + instagramUserId + "/media_publish";

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("creation_id", mediaId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, requestEntity, Map.class);
            if (response.getStatusCode() == HttpStatus.OK) {
                logger.info("Successfully published post with media ID: {}", mediaId);
                return true;
            } else {
                logger.error("Failed to publish post. Response: {}", response.getBody());
                return false;
            }
        } catch (HttpClientErrorException e) {
            logger.error("Instagram API error: {}", e.getResponseBodyAsString());
            throw new InstagramApiException("Instagram API error: " + e.getMessage());
        }
    }

    public void refreshAccessToken() {
        String url = API_URL + "/refresh_access_token?grant_type=ig_refresh_token&access_token=" + accessToken;

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, null, Map.class);
            if (response.getBody() != null && response.getBody().containsKey("access_token")) {
                accessToken = (String) response.getBody().get("access_token");
                logger.info("Instagram Access Token refreshed successfully");
            } else {
                logger.error("Failed to refresh Instagram Access Token");
            }
        } catch (Exception e) {
            logger.error("Error refreshing Instagram token: {}", e.getMessage());
        }
    }

}