package com.egemen.TweetBotTelegram.service.Impl;

import com.egemen.TweetBotTelegram.entity.InstagramPost;
import com.egemen.TweetBotTelegram.enums.PostStatus;
import com.egemen.TweetBotTelegram.repository.InstagramPostRepository;
import com.egemen.TweetBotTelegram.service.InstagramApiService;
import com.egemen.TweetBotTelegram.service.PexelsService;
import com.egemen.TweetBotTelegram.service.S3Service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class InstagramApiServiceImpl implements InstagramApiService {

    private final String accessToken;
    private final String userId;
    private final RestTemplate restTemplate;
    private final InstagramPostRepository instagramPostRepository;
    private final S3Service s3Service;
    private final PexelsService pexelsService;
    private static final String API_URL = "https://graph.instagram.com/v12.0";

    @Autowired
    public InstagramApiServiceImpl(
            String instagramAccessToken,
            String instagramUserId,
            InstagramPostRepository instagramPostRepository,
            S3Service s3Service,
            PexelsService pexelsService) {
        this.accessToken = instagramAccessToken;
        this.userId = instagramUserId;
        this.restTemplate = new RestTemplate();
        this.instagramPostRepository = instagramPostRepository;
        this.s3Service = s3Service;
        this.pexelsService = pexelsService;
        log.info("InstagramApiServiceImpl initialized with access token and user ID");
    }

    @Override
    public boolean createPost(InstagramPost post) {
        try {
            log.info("Creating Instagram post with caption: {}", post.getCaption());
            
            // Check if Instagram API credentials are configured
            if (accessToken == null || accessToken.isEmpty() || userId == null || userId.isEmpty()) {
                log.error("Instagram API credentials not configured");
                post.setErrorMessage("Instagram API credentials not configured");
                post.setPostStatus(PostStatus.FAILED);
                instagramPostRepository.save(post);
                return false;
            }
            
            // Check if image URL is valid
            String imageUrl = post.getImageUrl();
            if (imageUrl == null || imageUrl.isEmpty()) {
                // If no image URL is provided, use the image prompt to search for an image
                if (post.getImagePrompt() != null && !post.getImagePrompt().isEmpty()) {
                    log.info("No image URL provided, searching for image using prompt: {}", post.getImagePrompt());
                    imageUrl = pexelsService.searchImage(post.getImagePrompt());
                    post.setImageUrl(imageUrl);
                } else {
                    log.error("No image URL or prompt provided");
                    post.setErrorMessage("No image URL or prompt provided");
                    post.setPostStatus(PostStatus.FAILED);
                    instagramPostRepository.save(post);
                    return false;
                }
            } else if (!imageUrl.startsWith("http")) {
                // If the image URL is not a valid URL (e.g., it's a local path or a description),
                // use it as a search query to find an image
                log.error("Image URL must be a publicly accessible URL, not a local path: {}", imageUrl);
                String searchQuery = post.getImagePrompt();
                if (searchQuery == null || searchQuery.isEmpty()) {
                    searchQuery = "news";
                }
                imageUrl = pexelsService.searchImage(searchQuery);
                post.setImageUrl(imageUrl);
            }
            
            // Attempt to publish the post
            String postId = publishPostDirectly(post.getCaption(), imageUrl);
            
            if (postId != null) {
                post.setInstagramPostId(postId);
                post.setPostedAt(Timestamp.valueOf(LocalDateTime.now()));
                post.setPostStatus(PostStatus.POSTED);
                instagramPostRepository.save(post);
                return true;
            } else {
                post.setErrorMessage("Failed to publish post to Instagram");
                post.setPostStatus(PostStatus.FAILED);
                instagramPostRepository.save(post);
                return false;
            }
        } catch (Exception e) {
            log.error("Error creating Instagram post: {}", e.getMessage());
            post.setErrorMessage(e.getMessage());
            post.setPostStatus(PostStatus.FAILED);
            instagramPostRepository.save(post);
            return false;
        }
    }

    @Override
    public String uploadImage(String imageUrl) throws Exception {
        try {
            // Log the image URL for debugging
            log.info("Attempting to upload image to Instagram from URL: {}", imageUrl);
            
            // Change from using user ID to using 'me' endpoint
            String url = "https://graph.instagram.com/v12.0/me/media"; // Note: Changed to v12.0 which is more stable
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(accessToken);
            
            // Log the access token (first 10 chars only for security)
            if (accessToken != null && accessToken.length() > 10) {
                log.info("Using access token starting with: {}...", accessToken.substring(0, 10));
            } else {
                log.warn("Access token is null or too short");
            }

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("image_url", imageUrl);
            requestBody.put("is_carousel_item", false);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            // Log the request for debugging
            log.info("Sending request to Instagram API: {}", url);
            
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, request, Map.class);
            
            // Log the response for debugging
            log.info("Instagram API response status: {}", response.getStatusCode());
            if (response.getBody() != null) {
                log.info("Instagram API response body: {}", response.getBody());
            }
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                String mediaId = (String) response.getBody().get("id");
                log.info("Successfully created media with ID: {}", mediaId);
                return mediaId;
            }
            
            throw new Exception("Failed to get media ID from response");
        } catch (Exception e) {
            log.error("Error uploading image to Instagram: {}", e.getMessage());
            throw e;
        }
    }

    @Override
    public void publishPost(InstagramPost post) throws Exception {
        try {
            // Get the media ID from uploadImage
            log.info("Attempting to publish post with image URL: {}", post.getImageUrl());
            String mediaId = uploadImage(post.getImageUrl());
            log.info("Successfully uploaded image, received media ID: {}", mediaId);
            
            // Change from using user ID to using 'me' endpoint
            String url = "https://graph.instagram.com/v12.0/me/media_publish"; // Note: Changed to v12.0
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(accessToken);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("creation_id", mediaId);
            
            // Log the request for debugging
            log.info("Sending media_publish request to Instagram API with creation_id: {}", mediaId);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, request, Map.class);
            
            // Log the response for debugging
            log.info("Instagram API media_publish response status: {}", response.getStatusCode());
            if (response.getBody() != null) {
                log.info("Instagram API media_publish response body: {}", response.getBody());
            }
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                String instagramPostId = (String) response.getBody().get("id");
                post.setInstagramPostId(instagramPostId);
                post.setPostStatus(PostStatus.PUBLISHED);
                post.setPostedAt(Timestamp.valueOf(LocalDateTime.now()));
                instagramPostRepository.save(post);
                log.info("Successfully published post to Instagram with ID: {}", instagramPostId);
            } else {
                throw new Exception("Failed to publish post to Instagram");
            }
        } catch (Exception e) {
            log.error("Error publishing post to Instagram: {}", e.getMessage());
            throw e;
        }
    }

    @Override
    public void deletePost(String mediaId) throws Exception {
        try {
            String url = String.format("https://graph.instagram.com/v22.0/%s", mediaId);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(accessToken);

            Map<String, Object> requestBody = new HashMap<>();
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.DELETE, request, Map.class);
            
            if (response.getStatusCode() != HttpStatus.OK) {
                throw new Exception("Failed to delete post from Instagram");
            }
            
            log.info("Successfully deleted post from Instagram");
        } catch (Exception e) {
            log.error("Error deleting post from Instagram: {}", e.getMessage());
            throw e;
        }
    }

    @Override
    public InstagramPost getPostById(Long id) {
        return instagramPostRepository.findById(id).orElse(null);
    }

    private File downloadImageToTempFile(String imageUrl) {
        try {
            URL url = new URL(imageUrl);
            URLConnection connection = url.openConnection();
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");
            
            // Create a temporary file
            Path tempDir = Files.createTempDirectory("instagram_");
            File tempFile = new File(tempDir.toFile(), "image_" + UUID.randomUUID() + ".jpg");
            
            // Download the image
            try (InputStream inputStream = connection.getInputStream();
                 FileOutputStream outputStream = new FileOutputStream(tempFile)) {
                
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }
            
            return tempFile;
        } catch (Exception e) {
            log.error("Error downloading image: {}", e.getMessage());
            return null;
        }
    }

    private String publishPostDirectly(String caption, String imageUrl) {
        try {
            log.info("Attempting to publish post directly with image URL: {}", imageUrl);
            
            // Step 1: Create a container for the post
            String mediaContainerUrl = String.format("%s/me/media", API_URL);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("caption", caption);
            requestBody.put("image_url", imageUrl);
            
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(mediaContainerUrl)
                    .queryParam("access_token", accessToken);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> response = restTemplate.exchange(builder.toUriString(), HttpMethod.POST, request, Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                String mediaId = (String) response.getBody().get("id");
                log.info("Successfully created media container with ID: {}", mediaId);
                
                // Step 2: Publish the container
                String publishUrl = String.format("%s/me/media_publish", API_URL);
                
                Map<String, Object> publishRequestBody = new HashMap<>();
                publishRequestBody.put("creation_id", mediaId);
                
                UriComponentsBuilder publishBuilder = UriComponentsBuilder.fromHttpUrl(publishUrl)
                        .queryParam("access_token", accessToken);
                
                HttpEntity<Map<String, Object>> publishRequest = new HttpEntity<>(publishRequestBody, headers);
                ResponseEntity<Map> publishResponse = restTemplate.exchange(publishBuilder.toUriString(), HttpMethod.POST, publishRequest, Map.class);
                
                if (publishResponse.getStatusCode() == HttpStatus.OK && publishResponse.getBody() != null) {
                    String postId = (String) publishResponse.getBody().get("id");
                    log.info("Successfully published post with ID: {}", postId);
                    return postId;
                }
            }
            
            log.error("Failed to publish post directly");
            return null;
        } catch (Exception e) {
            log.error("Error publishing post directly: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public String createPostFromNews(String caption, String imageUrl) {
        try {
            // Create a temporary InstagramPost object
            InstagramPost post = new InstagramPost();
            post.setCaption(caption);
            post.setImageUrl(imageUrl);
            post.setCreatedAt(Timestamp.valueOf(LocalDateTime.now()));
            post.setPostStatus(PostStatus.PENDING);
            
            // Attempt to create the post
            if (createPost(post)) {
                return post.getInstagramPostId();
            } else {
                return null;
            }
        } catch (Exception e) {
            log.error("Error creating post from news: {}", e.getMessage());
            return null;
        }
    }
}