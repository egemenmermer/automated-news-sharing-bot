package com.egemen.TweetBotTelegram.service.Impl;

import com.egemen.TweetBotTelegram.service.ImageGenerationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;

@Service
public class ImageGenerationServiceImpl implements ImageGenerationService {
    private static final Logger log = LoggerFactory.getLogger(ImageGenerationServiceImpl.class);
    // Uses Hugging Face API to:
    // 1. Generate news-related images
    // 2. Add text overlays
    // 3. Process and save images
    private static final String HUGGING_FACE_API_URL = "https://api-inference.huggingface.co/models/CompVis/stable-diffusion-v1-4";
    
    @Value("${huggingface.api.key}")
    private String huggingFaceApiKey;

    private final RestTemplate restTemplate;

    public ImageGenerationServiceImpl(@Qualifier("imageRestTemplate") RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public String generateNewsImage(String newsTitle, String newsContent) {
        try {
            String prompt = createImagePrompt(newsTitle, newsContent);
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + huggingFaceApiKey);
            
            HttpEntity<String> request = new HttpEntity<>(prompt, headers);
            
            byte[] imageBytes = restTemplate.postForObject(
                HUGGING_FACE_API_URL, 
                request, 
                byte[].class
            );

            String outputPath = "generated-images/" + System.currentTimeMillis() + ".png";
            File outputFile = new File(outputPath);
            outputFile.getParentFile().mkdirs();
            
            BufferedImage originalImage = ImageIO.read(new java.io.ByteArrayInputStream(imageBytes));
            BufferedImage processedImage = addTextOverlay(originalImage, newsTitle);
            
            ImageIO.write(processedImage, "PNG", outputFile);

            return outputPath;
            
        } catch (Exception e) {
            log.error("Error generating news image: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public String createImagePrompt(String title, String content) {
        return "Create a professional news background image for: " + title + 
               ". Style: minimal, clean, suitable for news, abstract background";
    }

    @Override
    public BufferedImage addTextOverlay(BufferedImage originalImage, String newsTitle) {
        try {
            BufferedImage processedImage = new BufferedImage(
                originalImage.getWidth(),
                originalImage.getHeight(),
                BufferedImage.TYPE_INT_ARGB
            );

            Graphics2D g2d = processedImage.createGraphics();
            
            g2d.drawImage(originalImage, 0, 0, null);

            g2d.setRenderingHint(
                RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON
            );

            g2d.setColor(new Color(0, 0, 0, 0.5f));
            g2d.fillRect(0, originalImage.getHeight() - 200, originalImage.getWidth(), 200);

            Font font = new Font("Arial", Font.BOLD, 40);
            g2d.setFont(font);
            g2d.setColor(Color.WHITE);

            drawWrappedText(g2d, newsTitle, 30, originalImage.getHeight() - 150, originalImage.getWidth() - 60);

            g2d.dispose();

            return processedImage;

        } catch (Exception e) {
            log.error("Error processing image: {}", e.getMessage());
            return null;
        }
    }

    private void drawWrappedText(Graphics2D g2d, String text, int x, int y, int maxWidth) {
        FontMetrics fm = g2d.getFontMetrics();
        String[] words = text.split("\\s+");
        StringBuilder currentLine = new StringBuilder();
        int currentY = y;

        for (String word : words) {
            if (fm.stringWidth(currentLine + " " + word) < maxWidth) {
                if (currentLine.length() > 0) currentLine.append(" ");
                currentLine.append(word);
            } else {
                g2d.drawString(currentLine.toString(), x, currentY);
                currentY += fm.getHeight();
                currentLine = new StringBuilder(word);
            }
        }
        
        if (currentLine.length() > 0) {
            g2d.drawString(currentLine.toString(), x, currentY);
        }
    }
}