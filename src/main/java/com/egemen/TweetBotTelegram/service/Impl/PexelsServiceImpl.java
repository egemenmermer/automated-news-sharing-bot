package com.egemen.TweetBotTelegram.service.Impl;

import com.egemen.TweetBotTelegram.service.PexelsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;
import java.util.Random;

@Slf4j
@Service
public class PexelsServiceImpl implements PexelsService {

    @Value("${pexels.api.key}")
    private String apiKey;
    
    private final RestTemplate restTemplate;
    private final Random random = new Random();
    
    public PexelsServiceImpl() {
        this.restTemplate = new RestTemplate();
    }
    
    @Override
    public byte[] searchAndFetchImage(String query) throws Exception {
        log.info("Searching for image with query: {}", query);
        
        // Prepare headers with API key
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", apiKey);
        
        // Build the URL with the search query
        String url = String.format("https://api.pexels.com/v1/search?query=%s&per_page=10", query.replace(" ", "+"));
        
        // Make the API call
        ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class
        );
        
        // Process the response
        if (response.getBody() == null || !response.getBody().containsKey("photos")) {
            log.error("No photos found in Pexels API response");
            throw new Exception("No photos found in Pexels API response");
        }
        
        // Get the photos array
        Object photosObj = response.getBody().get("photos");
        if (!(photosObj instanceof java.util.List) || ((java.util.List<?>) photosObj).isEmpty()) {
            log.error("No photos found for query: {}", query);
            throw new Exception("No photos found for query: " + query);
        }
        
        // Select a random photo from the results
        java.util.List<?> photos = (java.util.List<?>) photosObj;
        int randomIndex = random.nextInt(photos.size());
        Map<String, Object> photo = (Map<String, Object>) photos.get(randomIndex);
        
        // Get the image URL (large size)
        Map<String, Object> src = (Map<String, Object>) photo.get("src");
        String imageUrl = (String) src.get("large");
        
        log.info("Selected image URL: {}", imageUrl);
        
        // Download the image
        return downloadImage(imageUrl);
    }
    
    private byte[] downloadImage(String imageUrl) throws Exception {
        log.info("Downloading image from URL: {}", imageUrl);
        
        URL url = new URL(imageUrl);
        URLConnection connection = url.openConnection();
        
        try (InputStream inputStream = connection.getInputStream();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            
            byte[] buffer = new byte[4096];
            int bytesRead;
            
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            
            byte[] imageData = outputStream.toByteArray();
            log.info("Successfully downloaded image, size: {} bytes", imageData.length);
            return imageData;
        } catch (Exception e) {
            log.error("Error downloading image: {}", e.getMessage());
            throw e;
        }
    }
}