package com.egemen.TweetBotTelegram.service;

import com.egemen.TweetBotTelegram.entity.Bot;
import com.egemen.TweetBotTelegram.entity.News;
import com.egemen.TweetBotTelegram.repository.BotRepository;
import com.egemen.TweetBotTelegram.repository.NewsRepository;
import com.egemen.TweetBotTelegram.service.Impl.NewsServiceImpl;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.*;

class NewsServiceImplTest {

    @Mock
    private NewsRepository newsRepository;
    @Mock
    private BotRepository botRepository;
    @Mock
    private RestTemplate restTemplate;
    @Mock
    private CircuitBreakerRegistry circuitBreakerRegistry;
    @Mock
    private MeterRegistry meterRegistry;

    private NewsServiceImpl newsService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        newsService = new NewsServiceImpl(
            newsRepository,
            botRepository,
            restTemplate,
            circuitBreakerRegistry,
            meterRegistry
        );
    }

    @Test
    void fetchAndSaveNews_Success() {
        // Arrange
        Bot mockBot = new Bot();
        mockBot.setId(1L);
        
        NewsResponseDTO mockResponse = new NewsResponseDTO();
        // Set up mock response

        when(botRepository.findById(1L)).thenReturn(Optional.of(mockBot));
        when(restTemplate.getForEntity(anyString(), any())).thenReturn(ResponseEntity.ok(mockResponse));
        when(newsRepository.saveAll(any())).thenReturn(List.of(new News()));

        // Act
        List<News> result = newsService.fetchAndSaveNews(1L);

        // Assert
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }
}