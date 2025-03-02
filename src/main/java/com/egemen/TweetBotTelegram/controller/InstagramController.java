package com.egemen.TweetBotTelegram.controller;

import com.egemen.TweetBotTelegram.entity.InstagramPost;
import com.egemen.TweetBotTelegram.service.InstagramApiService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/instagram")
public class InstagramController {

    private final InstagramApiService instagramApiService;

    public InstagramController(InstagramApiService instagramApiService) {
        this.instagramApiService = instagramApiService;
    }

    @PostMapping("/publish/{postId}")
    public ResponseEntity<String> publishPost(@PathVariable Long postId) {
        try {
            InstagramPost post = instagramApiService.getPostById(postId);
            if (post == null) {
                return ResponseEntity.notFound().build();
            }
            instagramApiService.publishPost(post);
            return ResponseEntity.ok("Post published successfully");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error publishing post: " + e.getMessage());
        }
    }

    @DeleteMapping("/{mediaId}")
    public ResponseEntity<String> deletePost(@PathVariable String mediaId) {
        try {
            instagramApiService.deletePost(mediaId);
            return ResponseEntity.ok("Post deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error deleting post: " + e.getMessage());
        }
    }
}