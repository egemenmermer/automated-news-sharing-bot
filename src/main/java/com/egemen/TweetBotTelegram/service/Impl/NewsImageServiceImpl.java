package com.egemen.TweetBotTelegram.service.Impl;

import com.egemen.TweetBotTelegram.entity.News;
import com.egemen.TweetBotTelegram.service.GeminiService;
import com.egemen.TweetBotTelegram.service.NewsImageService;
import com.egemen.TweetBotTelegram.service.PexelsService;
import com.egemen.TweetBotTelegram.service.S3Service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Slf4j
@Service
public class NewsImageServiceImpl implements NewsImageService {

    private final GeminiService geminiService;
    private final PexelsService pexelsService;
    private final S3Service s3Service;

    @Autowired
    public NewsImageServiceImpl(GeminiService geminiService, PexelsService pexelsService, S3Service s3Service) {
        this.geminiService = geminiService;
        this.pexelsService = pexelsService;
        this.s3Service = s3Service;
        log.info("NewsImageServiceImpl initialized");
    }

    @Override
    public String generateAndUploadImage(News news) {
        try {
            // Generate image prompt using Gemini
            String imagePrompt = geminiService.generateImageForNews(news);
            log.info("Generated image prompt: {}", imagePrompt);
            
            // Search for image using Pexels
            String imageUrl = pexelsService.searchImage(imagePrompt);
            log.info("Found image URL: {}", imageUrl);
            
            // Upload image to S3
            String s3Url = s3Service.uploadFileFromUrl(imageUrl);
            log.info("Uploaded image to S3: {}", s3Url);
            
            return s3Url;
        } catch (Exception e) {
            log.error("Error generating and uploading image: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public String generateImageForNews(News news) {
        try {
            if (news.getImageUrl() != null && !news.getImageUrl().isEmpty()) {
                // Upload the image to S3 and get a public URL
                String s3Url = s3Service.uploadFileFromUrl(news.getImageUrl());
                if (s3Url != null) {
                    return s3Url;
                }
            }
            
            // Fallback to a public placeholder
            return "https://via.placeholder.com/1080x1080.png?text=News+Image";
        } catch (Exception e) {
            log.error("Error generating image for news: {}", e.getMessage());
            return "https://via.placeholder.com/1080x1080.png?text=Error";
        }
    }

    @Override
    public String downloadImage(String imageUrl) {
        try {
            // Create directory if it doesn't exist
            Path directoryPath = Paths.get("generated-images");
            if (!Files.exists(directoryPath)) {
                Files.createDirectories(directoryPath);
            }

            // Generate a unique filename
            String filename = UUID.randomUUID().toString() + ".jpg";
            Path filePath = directoryPath.resolve(filename);

            // Download and save the image
            URL url = new URL(imageUrl);
            BufferedImage image = ImageIO.read(url);
            
            if (image != null) {
                File outputFile = filePath.toFile();
                ImageIO.write(image, "jpg", outputFile);
                return filePath.toString();
            } else {
                log.error("Failed to read image from URL: {}", imageUrl);
                return null;
            }
        } catch (IOException e) {
            log.error("Error downloading image: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public String generateNewsImage(String imagePrompt, String title) {
        try {
            // Always use a placeholder image for Instagram
            return "https://via.placeholder.com/1080x1080.png?text=" + 
                   title.substring(0, Math.min(20, title.length())).replace(" ", "+");
        } catch (Exception e) {
            log.error("Error generating image with prompt: {}", e.getMessage());
            return "https://via.placeholder.com/1080x1080.png?text=Error";
        }
    }
}