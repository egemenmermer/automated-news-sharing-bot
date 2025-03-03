package com.egemen.TweetBotTelegram.service;

import com.egemen.TweetBotTelegram.entity.InstagramPost;
import com.egemen.TweetBotTelegram.entity.News;
import com.egemen.TweetBotTelegram.enums.PostStatus;
import com.egemen.TweetBotTelegram.repository.InstagramPostRepository;
import com.egemen.TweetBotTelegram.service.Impl.InstagramApiServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class InstagramApiServiceTest {

    private InstagramApiService instagramApiService;

    @Mock
    private InstagramPostRepository instagramPostRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // Use mock values for the Instagram access token and user ID
        instagramApiService = new InstagramApiServiceImpl(
                "mock-access-token", 
                "mock-user-id", 
                instagramPostRepository);
    }

    @Test
    void createPost_withValidData_shouldReturnTrue() {
        // Test implementation
    }

    // Other test methods...
}
