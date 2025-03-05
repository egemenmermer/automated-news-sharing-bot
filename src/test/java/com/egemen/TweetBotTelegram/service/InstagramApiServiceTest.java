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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class InstagramApiServiceTest {

    @Mock
    private InstagramPostRepository instagramPostRepository;
    
    @Mock
    private S3Service s3Service;
    
    @Mock
    private PexelsService pexelsService;
    
    @Mock
    private ImageProcessingService imageProcessingService;
    
    @Mock
    private GeminiService geminiService;
    
    private InstagramApiService instagramApiService;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // Use mock values for the Instagram access token and user ID
        instagramApiService = new InstagramApiServiceImpl(
                "mock-access-token", 
                "mock-user-id", 
                instagramPostRepository,
                s3Service,
                pexelsService,
                imageProcessingService,
                geminiService
        );
    }

    @Test
    void testCreatePostFromNews() {
        // Setup
        String caption = "Test caption";
        String imageUrl = "https://example.com/image.jpg";
        
        // Mock the image processing service
        when(imageProcessingService.createNewsImageWithText(anyString(), anyString(), anyString()))
            .thenReturn(null); // Return null for simplicity in this test
            
        // Mock the S3 service
        when(s3Service.uploadFile(any(), anyString(), anyString()))
            .thenReturn("https://s3.example.com/processed-image.jpg");
        
        // Execute
        String result = instagramApiService.createPostFromNews(caption, imageUrl);
        
        // Verify
        assertNull(result); // Since we're not actually making API calls in the test
        
        // Verify interactions
        verify(imageProcessingService).createNewsImageWithText(eq(imageUrl), anyString(), eq(caption));
        // Add more verifications as needed
    }

    // Other test methods...
}
