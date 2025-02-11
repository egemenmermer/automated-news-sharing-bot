package com.egemen.TweetBotTelegram.controller;

import com.egemen.TweetBotTelegram.entity.News;
import com.egemen.TweetBotTelegram.service.NewsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/news")
public class NewsController {

    private final NewsService newsService;

    public NewsController(NewsService newsService) {
        this.newsService = newsService;
    }

    @GetMapping
    public ResponseEntity<List<News>> getAllNews() {
        return ResponseEntity.ok(newsService.getAllNews());
    }

    @GetMapping("/unprocessed")
    public ResponseEntity<List<News>> getUnprocessedNews() {
        return ResponseEntity.ok(newsService.getUnprocessedNews());
    }

    @GetMapping("/{id}")
    public ResponseEntity<News> getNews(@PathVariable Long id) {
        return ResponseEntity.ok(newsService.getNews(id));
    }

    @PostMapping("/fetch")
    public ResponseEntity<String> fetchNews() {
        newsService.fetchLatestNews();
        return ResponseEntity.ok("News fetch initiated");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNews(@PathVariable Long id) {
        newsService.deleteNews(id);
        return ResponseEntity.ok().build();
    }
}