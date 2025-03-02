package com.egemen.TweetBotTelegram.controller;

import com.egemen.TweetBotTelegram.entity.Bot;
import com.egemen.TweetBotTelegram.service.BotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bots")
@Tag(name = "Bot Controller", description = "Bot Management")
public class BotController {
    private final BotService botService;

    public BotController(BotService botService) {
        this.botService = botService;
    }

    @PostMapping("/create")
    @Operation(summary = "Create Bot")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Bot created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "500", description = "Network error occurred")})
    public ResponseEntity<Bot> createBot(@RequestBody Bot bot) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(botService.createBot(bot));
    }

    @GetMapping("/list")
    @Operation(summary = "List All Bots")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Bots fetched successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "500", description = "Network error occurred")})
    public ResponseEntity<List<Bot>> listBots() {
        return ResponseEntity.status(HttpStatus.OK).body(botService.listBots());
    }
}
