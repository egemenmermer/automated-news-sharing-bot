package com.egemen.TweetBotTelegram.service.Impl;

import com.egemen.TweetBotTelegram.entity.News;
import com.egemen.TweetBotTelegram.service.ImageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.net.URL;
import java.io.ByteArrayInputStream;

@Service
public class ImageServiceImpl implements ImageService {
    private static final Logger log = LoggerFactory.getLogger(ImageServiceImpl.class);
    private static final String PEXELS_API_URL = "https://api.pexels.com/v1/search";
    
    private final RestTemplate restTemplate;
    private final String pexelsApiKey;

    public ImageServiceImpl(
            RestTemplate restTemplate,
            @Value("${pexels.api.key}") String pexelsApiKey) {
        this.restTemplate = restTemplate;
        this.pexelsApiKey = pexelsApiKey;
    }

    @Override
    public String findImageForNews(News news) {
        try {
            if (news.getImageUrl() != null && isValidForInstagram(news.getImageUrl())) {
                return news.getImageUrl();
            }
            return searchPexelsImage(news.getTitle());
        } catch (Exception e) {
            log.error("Error finding image for news: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public String createSearchQuery(String title, String content) {
        return title.replaceAll("[^a-zA-Z0-9\\s]", "")
                   .trim()
                   .replaceAll("\\s+", "+");
    }

    @Override
    public String searchPexelsImage(String query) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", pexelsApiKey);
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            String url = PEXELS_API_URL + "?query=" + createSearchQuery(query, "") + "&per_page=1";
            
            var response = restTemplate.getForObject(url, Object.class);
            // TODO: Parse response and extract image URL
            return null;
        } catch (Exception e) {
            log.error("Error searching Pexels: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public boolean isValidForInstagram(String imageUrl) {
        return validateImage(imageUrl, 320, 320);
    }

    @Override
    public String processImageForInstagram(String imageUrl) {
        try {
            // For now, just validate the image
            if (isValidForInstagram(imageUrl)) {
                return imageUrl;
            }
            return null;
        } catch (Exception e) {
            log.error("Error processing image for Instagram: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public boolean validateImage(String imageUrl, int minWidth, int minHeight) {
        try {
            InputStream imageStream = downloadImage(imageUrl);
            if (imageStream == null) return false;

            BufferedImage image = ImageIO.read(imageStream);
            if (image == null) return false;
            
            return image.getWidth() >= minWidth && image.getHeight() >= minHeight;
        } catch (Exception e) {
            log.error("Error validating image: {}", e.getMessage());
            return false;
        }
    }

    private InputStream downloadImage(String imageUrl) {
        try {
            byte[] imageBytes = restTemplate.getForObject(imageUrl, byte[].class);
            if (imageBytes == null) {
                log.error("Error downloading image {}: null response", imageUrl);
                return null;
            }
            return new ByteArrayInputStream(imageBytes);
        } catch (Exception e) {
            log.error("Error downloading image {}: {}", imageUrl, e.getMessage());
            return null;
        }
    }
}