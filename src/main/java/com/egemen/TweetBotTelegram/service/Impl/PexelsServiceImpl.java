package com.egemen.TweetBotTelegram.service.Impl;

import com.egemen.TweetBotTelegram.service.PexelsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Slf4j
@Service
public class PexelsServiceImpl implements PexelsService {

    private final String apiKey;
    private final RestTemplate restTemplate;
    private static final String PEXELS_API_URL = "https://api.pexels.com/v1/search";
    private static final String[] FALLBACK_IMAGES = {
        "https://images.pexels.com/photos/3184360/pexels-photo-3184360.jpeg",
        "https://images.pexels.com/photos/3184339/pexels-photo-3184339.jpeg",
        "https://images.pexels.com/photos/3182812/pexels-photo-3182812.jpeg",
        "https://images.pexels.com/photos/3182777/pexels-photo-3182777.jpeg",
        "https://images.pexels.com/photos/3182774/pexels-photo-3182774.jpeg"
    };

    @Autowired
    public PexelsServiceImpl(String pexelsApiKey) {
        this.apiKey = pexelsApiKey;
        this.restTemplate = new RestTemplate();
        log.info("PexelsServiceImpl initialized with API key");
    }
    
    @Override
    public String searchImage(String query) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", apiKey);
            
            String url = PEXELS_API_URL + "?query=" + query + "&per_page=1";
            
            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                if (responseBody.containsKey("photos")) {
                    List<Map<String, Object>> photos = (List<Map<String, Object>>) responseBody.get("photos");
                    if (photos != null && !photos.isEmpty()) {
                        Map<String, Object> photo = photos.get(0);
                        Map<String, Object> src = (Map<String, Object>) photo.get("src");
                        return (String) src.get("large");
                    }
                }
            }
            
            // If no image found, return a fallback image
            return getFallbackImage();
        } catch (Exception e) {
            log.error("Error searching image from Pexels: {}", e.getMessage());
            return getFallbackImage();
        }
    }
    
    private String getFallbackImage() {
        // Return a random fallback image
        return FALLBACK_IMAGES[new Random().nextInt(FALLBACK_IMAGES.length)];
    }
    
    @Override
    public byte[] searchAndFetchImage(String query) throws Exception {
        String imageUrl = searchImage(query);
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