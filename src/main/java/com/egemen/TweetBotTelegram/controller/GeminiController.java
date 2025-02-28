package com.egemen.TweetBotTelegram.controller;

import com.egemen.TweetBotTelegram.dto.SummarizedNewsDTO;
import com.egemen.TweetBotTelegram.dto.TweetResponseDTO;
import com.egemen.TweetBotTelegram.entity.SummarizedNews;
import com.egemen.TweetBotTelegram.entity.Tweet;
import com.egemen.TweetBotTelegram.exception.CustomException;
import com.egemen.TweetBotTelegram.service.GeminiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/gemini")
@RequiredArgsConstructor
public class GeminiController {

    @Autowired
    private GeminiService geminiService;

    @PostMapping("/summarize")
    @Operation(summary = "Summarize News")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Summarized successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "500", description = "Network error occurred")})
    public ResponseEntity<List<SummarizedNewsDTO>> summarizeNews() throws InterruptedException {
        List<SummarizedNews> summarizedNews = geminiService.summarize();
        if(summarizedNews.isEmpty()) {
            throw new CustomException(new Exception("No summarized news found"), HttpStatus.NOT_FOUND);
        }
        //map to dto
        Map<String, SummarizedNews> uniqueSummarizedNewsMap = summarizedNews.stream()
                .collect(java.util.stream.Collectors.toMap(SummarizedNews::getContent, summarizeNews -> summarizeNews, (existing, replacement) -> existing));


        List<SummarizedNewsDTO> summarizedNewsDTO = uniqueSummarizedNewsMap.values().stream()
                .map(summarizedNewsResponse -> new SummarizedNewsDTO(
                        summarizedNewsResponse.getContent(),
                        summarizedNewsResponse.getSummarizedCount(),
                        summarizedNewsResponse.getStatus()
                ))
                .toList();

        return ResponseEntity.ok(summarizedNewsDTO);
    }

    @PostMapping("/generate")
    @Operation(summary = "Generate Tweets")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Tweets generated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "500", description = "Network error occurred")})
    public ResponseEntity<List<TweetResponseDTO>> generateTweets() throws InterruptedException {
        List<Tweet> tweets = geminiService.generate();
        if(tweets.isEmpty()) {
            throw new CustomException(new Exception("No tweets generated"), HttpStatus.NOT_FOUND);
        }
        Map<String, Tweet> uniqueTweets = tweets.stream()
                .collect(java.util.stream.Collectors.toMap(Tweet::getContent, tweet -> tweet, (existing, replacement) -> existing));
        //map to dto
        List<TweetResponseDTO> tweetResponseDTOs = uniqueTweets.values().stream()
                .map(tweet -> new TweetResponseDTO(
                        tweet.getContent(),
                        tweet.getStatus(),
                        tweet.getScheduledAt()
                ))
                .collect(java.util.stream.Collectors.toList());

        return ResponseEntity.ok(tweetResponseDTOs);
    }

    @PostMapping("/generate-summary")
    public ResponseEntity<String> generateSummary(
            @RequestParam String title,
            @RequestParam String content) {
        return ResponseEntity.ok(geminiService.generateSummary(title, content));
    }

    @PostMapping("/generate-image-prompt")
    public ResponseEntity<String> generateImagePrompt(
            @RequestParam String title,
            @RequestParam String content) {
        return ResponseEntity.ok(geminiService.generateImagePrompt(title, content));
    }

    @PostMapping("/generate-caption")
    public ResponseEntity<String> generateCaption(
            @RequestParam String title,
            @RequestParam String content) {
        return ResponseEntity.ok(geminiService.generateCaption(title, content));
    }

    @PostMapping("/generate")
    public ResponseEntity<String> generateResponse(@RequestParam String prompt) {
        return ResponseEntity.ok(geminiService.generateResponse(prompt));
    }
} 