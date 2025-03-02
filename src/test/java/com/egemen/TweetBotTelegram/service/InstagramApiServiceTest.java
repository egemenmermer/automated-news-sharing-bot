package com.egemen.TweetBotTelegram.service;

import com.egemen.TweetBotTelegram.entity.Bot;
import com.egemen.TweetBotTelegram.entity.InstagramPost;
import com.egemen.TweetBotTelegram.entity.News;
import com.egemen.TweetBotTelegram.enums.NewsStatus;
import com.egemen.TweetBotTelegram.enums.PostStatus;
import com.egemen.TweetBotTelegram.repository.InstagramPostRepository;
import com.egemen.TweetBotTelegram.service.Impl.InstagramApiServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.client.RestTemplate;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class InstagramApiServiceTest {

    @Mock
    private InstagramPostRepository instagramPostRepository;

    private InstagramApiService instagramApiService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        instagramApiService = new InstagramApiServiceImpl(instagramPostRepository);
    }

    @Test
    void testCreatePost() {
        // Given
        InstagramPost post = new InstagramPost();
        post.setCaption("Test caption");
        post.setImageUrl("http://example.com/image.jpg");
        post.setPostStatus(PostStatus.PENDING);
        post.setCreatedAt(Timestamp.valueOf(LocalDateTime.now()));

        when(instagramPostRepository.save(any(InstagramPost.class))).thenReturn(post);

        // When
        boolean result = instagramApiService.createPost(post);

        // Then
        assertFalse(result); // Should be false since we're not actually connecting to Instagram API in tests
    }
}
