package com.egemen.TweetBotTelegram.service.Impl;

import com.egemen.TweetBotTelegram.entity.InstagramPost;
import com.egemen.TweetBotTelegram.enums.PostStatus;
import com.egemen.TweetBotTelegram.repository.InstagramPostRepository;
import com.egemen.TweetBotTelegram.service.ImageProcessingService;
import com.egemen.TweetBotTelegram.service.InstagramApiService;
import com.egemen.TweetBotTelegram.service.PexelsService;
import com.egemen.TweetBotTelegram.service.S3Service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.nio.file.Files;
import java.util.Base64;
import java.awt.image.BufferedImage;
import java.awt.Font;
import java.awt.Color;
import org.apache.commons.lang3.StringUtils;
import com.egemen.TweetBotTelegram.service.GeminiService;

@Slf4j
@Service
public class InstagramApiServiceImpl implements InstagramApiService {

    private final String accessToken;
    private final String userId;
    private final RestTemplate restTemplate;
    private final InstagramPostRepository instagramPostRepository;
    private final S3Service s3Service;
    private final PexelsService pexelsService;
    private final ImageProcessingService imageProcessingService;
    private final GeminiService geminiService;
    private static final String API_URL = "https://graph.instagram.com/v12.0";

    @Autowired
    public InstagramApiServiceImpl(
            String instagramAccessToken,
            String instagramUserId,
            InstagramPostRepository instagramPostRepository,
            S3Service s3Service,
            PexelsService pexelsService,
            ImageProcessingService imageProcessingService,
            GeminiService geminiService) {
        this.accessToken = instagramAccessToken;
        this.userId = instagramUserId;
        this.restTemplate = new RestTemplate();
        this.instagramPostRepository = instagramPostRepository;
        this.s3Service = s3Service;
        this.pexelsService = pexelsService;
        this.imageProcessingService = imageProcessingService;
        this.geminiService = geminiService;
        log.info("InstagramApiServiceImpl initialized with access token and user ID");
    }

    @Override
    public boolean createPost(InstagramPost post) {
        try {
            log.info("Creating Instagram post with caption: {}", post.getCaption());
            
            // Check if Instagram API credentials are configured
            if (accessToken == null || accessToken.isEmpty() || userId == null || userId.isEmpty()) {
                log.error("Instagram API credentials not configured");
                saveFailedPost(post, "Instagram API credentials not configured");
                return false;
            }
            
            // Check if image URL is valid and get a proper image URL if needed
            String imageUrl = post.getImageUrl();
            if (imageUrl == null || imageUrl.isEmpty() || !imageUrl.startsWith("http")) {
                // If no valid image URL, try to get one from Pexels using the image prompt
                String searchQuery = post.getImagePrompt();
                if (searchQuery == null || searchQuery.isEmpty()) {
                    searchQuery = post.getTitle();
                }
                
                // Remove "Image prompt:" prefix if present
                if (searchQuery != null && searchQuery.startsWith("Image prompt:")) {
                    searchQuery = searchQuery.substring("Image prompt:".length()).trim();
                }
                
                log.info("Searching for image with query: {}", searchQuery);
                imageUrl = pexelsService.searchImage(searchQuery);
                
                if (imageUrl == null || imageUrl.isEmpty()) {
                    log.error("Failed to find a suitable image");
                    saveFailedPost(post, "Failed to find a suitable image");
                    return false;
                }
                
                // Update the post with the new image URL
                post.setImageUrl(imageUrl);
            }

            // Generate two different summaries
            String imageText = geminiService.generateDetailedSummary(
                post.getTitle(), 
                post.getContent() != null ? post.getContent() : post.getTitle(),
                400  // Longer summary for image
            );

            if (imageText == null) {
                // If Gemini fails, use the original content
                imageText = post.getTitle() + "\n\n" + 
                    (post.getContent() != null ? post.getContent() : "");
            }

            String captionText = geminiService.generateShortSummary(
                post.getTitle(),
                150  // Shorter for caption
            );

            if (captionText == null) {
                // If Gemini fails, use a shortened version of the title
                captionText = StringUtils.abbreviate(post.getTitle(), 150);
            }

            // Create image with detailed text
            File processedImageFile = imageProcessingService.createNewsImageWithText(
                imageUrl,
                post.getTitle(),
                imageText  // Use detailed summary on image
            );
            
            if (processedImageFile == null) {
                saveFailedPost(post, "Failed to process image");
                return false;
            }

            // Use shorter text for Instagram caption
            String finalCaption = captionText + 
                (post.getUrl() != null ? "\n\nRead more: " + post.getUrl() : "") + 
                "\n\n#tech #technology #technews #innovation";

            try {
                // Create the container
                String containerId = createMediaContainer(processedImageFile, finalCaption);
                if (containerId == null) {
                    saveFailedPost(post, "Failed to create media container");
                    return false;
                }

                // Wait for container to be ready
                Thread.sleep(5000);

                // Publish the container
                String mediaId = publishMediaContainer(containerId);
                if (mediaId == null) {
                    saveFailedPost(post, "Failed to publish media container");
                    return false;
                }

                // Update post status
                post.setInstagramPostId(mediaId);
                post.setPostStatus(PostStatus.POSTED);
                post.setPostedAt(new Timestamp(System.currentTimeMillis()));
                instagramPostRepository.save(post);
                
                return true;

            } finally {
                // Clean up temp file
                if (processedImageFile != null && processedImageFile.exists()) {
                    processedImageFile.delete();
                }
            }
            
        } catch (Exception e) {
            log.error("Error creating post: {}", e.getMessage(), e);
            saveFailedPost(post, e.getMessage());
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

    // Extract a subtitle from the caption (first sentence or first 100 chars)
    private String extractSubtitle(String caption) {
        if (caption == null || caption.isEmpty()) {
            return "";
        }
        
        // Try to get the first sentence
        int endOfSentence = caption.indexOf('.');
        if (endOfSentence > 0 && endOfSentence < 100) {
            return caption.substring(0, endOfSentence + 1);
        }
        
        // Otherwise get first 100 chars or less
        return caption.length() > 100 ? caption.substring(0, 97) + "..." : caption;
    }

    private String uploadImageToInstagram(String imageUrl) {
        try {
            log.info("Uploading image to Instagram: {}", imageUrl);
            
            // Download the image to a temporary file
            URL url = new URL(imageUrl);
            File tempFile = File.createTempFile("instagram_upload_", ".jpg");
            
            try (InputStream in = url.openStream();
                 FileOutputStream out = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }
            
            // Now upload the local file
            String mediaContainerId = uploadMediaContainer(tempFile);
            
            // Clean up
            tempFile.delete();
            
            return mediaContainerId;
        } catch (Exception e) {
            log.error("Error uploading image to Instagram: {}", e.getMessage(), e);
            return null;
        }
    }

    private String uploadMediaContainer(File imageFile) {
        try {
            // Create multipart request
            RestTemplate restTemplate = new RestTemplate();
            String url = API_URL + "/" + userId + "/media";
            
            // Create multipart file
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("image_url", new FileSystemResource(imageFile));
            body.add("caption", "");
            body.add("access_token", accessToken);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            
            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                Map<String, Object> responseBody = response.getBody();
                return (String) responseBody.get("id");
            } else {
                log.error("Failed to upload media container: {}", response.getBody());
                return null;
            }
        } catch (Exception e) {
            log.error("Error uploading media container: {}", e.getMessage(), e);
            return null;
        }
    }

    private String createMediaContainer(File imageFile, String caption) {
        try {
            String url = "https://graph.instagram.com/v12.0/me/media";
            
            // Upload to S3 and get a pre-signed URL that's valid for 1 hour
            String s3Key = "instagram_" + UUID.randomUUID().toString() + ".jpg";
            byte[] imageBytes = Files.readAllBytes(imageFile.toPath());
            String imageUrl = s3Service.uploadFileAndGetPresignedUrl(imageBytes, s3Key, "image/jpeg", 3600); // 1 hour expiry
            
            if (imageUrl == null || imageUrl.isEmpty()) {
                log.error("Failed to upload image to S3");
                return null;
            }
            
            log.info("Image uploaded to S3 with presigned URL: {}", imageUrl);
            
            // Build URL with access token as query parameter
            url = UriComponentsBuilder.fromHttpUrl(url)
                .queryParam("access_token", accessToken)
                .toUriString();
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            
            // Create form data
            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("image_url", imageUrl);
            body.add("caption", caption);
            body.add("media_type", "IMAGE");
            
            HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(body, headers);
            
            log.info("Sending request to Instagram API: {} with image URL: {}", url, imageUrl);
            
            ResponseEntity<Map> response = restTemplate.postForEntity(url, requestEntity, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String containerId = (String) response.getBody().get("id");
                log.info("Successfully created media container with ID: {}", containerId);
                
                // Wait a bit for the container to be ready
                Thread.sleep(5000);
                
                return containerId;
            }
            
            log.error("Failed to create media container: {}", response.getBody());
            return null;
            
        } catch (Exception e) {
            log.error("Error creating media container: {}", e.getMessage(), e);
            return null;
        }
    }

    private String publishMediaContainer(String containerId) {
        try {
            Thread.sleep(3000);
            
            String url = "https://graph.instagram.com/v12.0/me/media_publish";
            
            // Build URL with access token as query parameter
            url = UriComponentsBuilder.fromHttpUrl(url)
                .queryParam("access_token", accessToken)
                .toUriString();
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            Map<String, Object> body = new HashMap<>();
            body.put("creation_id", containerId);
            
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            
            log.info("Publishing media container with ID: {}", containerId);
            
            ResponseEntity<Map> response = restTemplate.postForEntity(url, requestEntity, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String mediaId = (String) response.getBody().get("id");
                log.info("Successfully published media with ID: {}", mediaId);
                return mediaId;
            }
            
            log.error("Failed to publish media: {}", response.getBody());
            return null;
            
        } catch (Exception e) {
            log.error("Error publishing media: {}", e.getMessage(), e);
            return null;
        }
    }

    private void saveFailedPost(InstagramPost post, String errorMessage) {
        try {
            post.setErrorMessage(errorMessage);
            post.setPostStatus(PostStatus.FAILED);
            post.setRetryCount(post.getRetryCount() + 1);
            instagramPostRepository.save(post);
            log.info("Saved failed post status to database");
        } catch (Exception ex) {
            log.error("Failed to save error status to database: {}", ex.getMessage(), ex);
        }
    }
}