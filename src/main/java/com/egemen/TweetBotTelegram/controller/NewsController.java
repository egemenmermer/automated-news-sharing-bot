package com.egemen.TweetBotTelegram.controller;

import com.egemen.TweetBotTelegram.dto.NewsListDTO;
import com.egemen.TweetBotTelegram.entity.News;
import com.egemen.TweetBotTelegram.service.NewsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/news")
public class NewsController {

    private final NewsService newsService;

    public NewsController(NewsService newsService) {
        this.newsService = newsService;
    }

    @GetMapping("/fetch")
    @Operation(summary = "Fetch News")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "News fetched successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "500", description = "Network error occurred")})
    public ResponseEntity<List<NewsListDTO>> fetchNews(
            @Parameter(description = "Bot Id for news fetching", required = true)
            @RequestParam Long botId) {
        List<News> newsList = newsService.fetchAndSaveNews(botId, false);
        Map<String, News> uniqueNewsMap = newsList.stream()
                .collect(Collectors.toMap(News::getTitle, news -> news, (existing, replacement) -> existing));

        // Map'teki haberleri NewsListDTO'ya dönüştür
        List<NewsListDTO> newsListDTO = uniqueNewsMap.values().stream()
                .map(news -> new NewsListDTO(
                        news.getTitle(),
                        news.getContent(),
                        news.getPublishedAt(),
                        news.getStatus()
                ))
                .collect(Collectors.toList());


        return ResponseEntity.ok(newsListDTO);
    }

    @GetMapping("/fetchTR")
    @Operation(summary = "Fetch News TR")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "TR News fetched successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "500", description = "Network error occurred")})
    public ResponseEntity<List<NewsListDTO>> fetchNewsTR(
            @Parameter(description = "Bot Id for news fetching", required = true)
            @RequestParam Long botId) {
        List<News> newsList = newsService.fetchAndSaveNews(botId, true);
        Map<String, News> uniqueNewsMap = newsList.stream()
                .collect(Collectors.toMap(News::getTitle, news -> news, (existing, replacement) -> existing));

        // Map'teki haberleri NewsListDTO'ya dönüştür
        List<NewsListDTO> newsListDTO = uniqueNewsMap.values().stream()
                .map(news -> new NewsListDTO(
                        news.getTitle(),
                        news.getContent(),
                        news.getPublishedAt(),
                        news.getStatus()
                ))
                .collect(Collectors.toList());

        // DTO listesine ResponseEntity ile dönüş yap
        return ResponseEntity.ok(newsListDTO);
    }

    @GetMapping("/list")
    @Operation(summary = "List all news")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "News listed successfully"),
            @ApiResponse(responseCode = "500", description = "Server error occurred")})
    public ResponseEntity<List<NewsListDTO>> listNews() {
        List<News> newsList = newsService.getAllNews();
        List<NewsListDTO> newsListDTO = newsList.stream()
                .map(news -> new NewsListDTO(
                        news.getTitle(),
                        news.getContent(),
                        news.getPublishedAt(),
                        news.getStatus()
                ))
                .collect(Collectors.toList());
        return ResponseEntity.ok(newsListDTO);
    }
}