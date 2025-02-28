package com.egemen.TweetBotTelegram.controller;

import com.egemen.TweetBotTelegram.service.InstagramApiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/instagram")
public class InstagramController {

    @Autowired
    private InstagramApiService instagramApiService;

    @PostMapping("/post")
    @Operation(summary = "Post to Instagram")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Tweet generated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "500", description = "Network error occurred")})
    public ResponseEntity<String> processAndPostToInstagram(
            @Parameter(description = "Bot Id for Instagram Posting", required = true)
            @RequestParam Long botId) {
        instagramApiService.processAndPostToInstagram(botId.intValue());
        return ResponseEntity.ok("Posted");
    }
}