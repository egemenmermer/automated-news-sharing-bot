package com.egemen.TweetBotTelegram.service.Impl;

import com.egemen.TweetBotTelegram.dto.instagram.InstagramMediaResponse;
import com.egemen.TweetBotTelegram.dto.instagram.InstagramErrorResponse;
import com.egemen.TweetBotTelegram.exception.InstagramApiException;
import com.egemen.TweetBotTelegram.service.InstagramApiService;
import com.egemen.TweetBotTelegram.service.RateLimiterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Service
public class InstagramApiServiceImpl implements InstagramApiService {
    private static final Logger log = LoggerFactory.getLogger(InstagramApiServiceImpl.class);

    @Value("${instagram.access.token}")
    private String accessToken;

    @Value("${instagram.api.url}")
    private String apiUrl;

    @Value("${instagram.rate.limit.capacity:25}")
    private int rateLimitCapacity;

    @Value("${instagram.rate.limit.interval:3600}")
    private long rateLimitInterval;

    private final RestTemplate restTemplate;
    private final RateLimiterService rateLimiterService;

    public InstagramApiServiceImpl(RestTemplate restTemplate, RateLimiterService rateLimiterService) {
        this.restTemplate = restTemplate;
        this.rateLimiterService = rateLimiterService;
    }

    @Override
    @Retryable(
        value = {InstagramApiException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 5000)
    )
    public String uploadMedia(String imageUrl) {
        try {
            rateLimiterService.checkRateLimit("instagram-upload", rateLimitCapacity, rateLimitInterval);
            
            String url = apiUrl + "/media";
            
            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("image_url", imageUrl);
            body.add("access_token", accessToken);
            body.add("media_type", "IMAGE");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
            
            ResponseEntity<InstagramMediaResponse> response = restTemplate.postForEntity(
                url, 
                request, 
                InstagramMediaResponse.class
            );

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new InstagramApiException("Failed to upload media: " + response.getStatusCode());
            }

            return response.getBody().getId();
            
        } catch (HttpClientErrorException e) {
            log.error("Instagram API error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new InstagramApiException("Instagram API error: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error uploading media to Instagram: {}", e.getMessage());
            throw new InstagramApiException("Failed to upload media to Instagram", e);
        }
    }

    @Override
    @Retryable(
        value = {InstagramApiException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 5000)
    )
    public String publishMedia(String containerId, String caption) {
        try {
            rateLimiterService.checkRateLimit("instagram-publish", rateLimitCapacity, rateLimitInterval);
            
            String url = apiUrl + "/media/publish";
            
            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("creation_id", containerId);
            body.add("access_token", accessToken);
            body.add("caption", caption);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
            
            ResponseEntity<InstagramMediaResponse> response = restTemplate.postForEntity(
                url,
                request,
                InstagramMediaResponse.class
            );

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new InstagramApiException("Failed to publish media");
            }

            return response.getBody().getId();
            
        } catch (HttpClientErrorException e) {
            log.error("Instagram API error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new InstagramApiException("Instagram API error: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error publishing media to Instagram: {}", e.getMessage());
            throw new InstagramApiException("Failed to publish media to Instagram", e);
        }
    }

    @Override
    public String getMediaStatus(String containerId) {
        try {
            String url = apiUrl + "/media/" + containerId;
            
            ResponseEntity<InstagramMediaResponse> response = restTemplate.getForEntity(
                url + "?access_token={token}",
                InstagramMediaResponse.class,
                accessToken
            );

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new InstagramApiException("Failed to get media status");
            }

            return response.getBody().getStatus();
            
        } catch (HttpClientErrorException e) {
            log.error("Instagram API error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new InstagramApiException("Instagram API error: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error checking media status: {}", e.getMessage());
            throw new InstagramApiException("Failed to check media status", e);
        }
    }
}