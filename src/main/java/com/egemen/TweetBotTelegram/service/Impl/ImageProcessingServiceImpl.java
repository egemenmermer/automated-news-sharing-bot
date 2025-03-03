package com.egemen.TweetBotTelegram.service.Impl;

import com.egemen.TweetBotTelegram.service.ImageProcessingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.LineBreakMeasurer;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.AttributedString;
import java.util.UUID;

@Slf4j
@Service
public class ImageProcessingServiceImpl implements ImageProcessingService {

    private static final int TARGET_WIDTH = 1080;
    private static final int TARGET_HEIGHT = 1080;
    private static final Color OVERLAY_COLOR = new Color(0, 0, 0, 180);
    private static final Color TEXT_COLOR = Color.WHITE;
    private static final Color ACCENT_COLOR = new Color(255, 50, 50);
    private static final String LOGO_TEXT = "NEURAL NEWS";
    private static final int PADDING = 40;
    private static final int TITLE_MAX_WIDTH = TARGET_WIDTH - (PADDING * 2);

    @Override
    public File createNewsImageWithText(String imageUrl, String title, String subtitle) {
        try {
            // Download the image
            BufferedImage sourceImage = downloadImage(imageUrl);
            
            // Resize if needed
            BufferedImage resizedImage = resizeImage(sourceImage);
            
            // Add text overlay
            BufferedImage processedImage = addTextToImage(resizedImage, title, subtitle);
            
            // Save to temp file
            File outputFile = File.createTempFile("news_image_", ".jpg");
            ImageIO.write(processedImage, "jpg", outputFile);
            
            log.info("Created news image with text overlay: {}", outputFile.getAbsolutePath());
            return outputFile;
            
        } catch (Exception e) {
            log.error("Error creating news image with text: {}", e.getMessage(), e);
            return null;
        }
    }

    @Override
    public BufferedImage addTextToImage(BufferedImage sourceImage, String title, String subtitle) {
        int width = sourceImage.getWidth();
        int height = sourceImage.getHeight();
        
        // Create a new image with the same dimensions
        BufferedImage resultImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = resultImage.createGraphics();
        
        // Enable anti-aliasing for smoother text
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        
        // Draw the original image
        g2d.drawImage(sourceImage, 0, 0, null);
        
        // Add semi-transparent overlay for better text readability
        g2d.setColor(OVERLAY_COLOR);
        g2d.fillRect(0, height / 2, width, height / 2);
        
        // Add logo at the top
        Font logoFont = new Font("Arial", Font.BOLD, 36);
        g2d.setFont(logoFont);
        g2d.setColor(ACCENT_COLOR);
        
        FontMetrics logoMetrics = g2d.getFontMetrics(logoFont);
        int logoX = (width - logoMetrics.stringWidth(LOGO_TEXT)) / 2;
        int logoY = PADDING + logoMetrics.getAscent();
        
        g2d.drawString(LOGO_TEXT, logoX, logoY);
        
        // Draw title text
        if (title != null && !title.isEmpty()) {
            Font titleFont = new Font("Arial", Font.BOLD, 48);
            drawWrappedText(g2d, title, titleFont, TEXT_COLOR, PADDING, height / 2 + PADDING, TITLE_MAX_WIDTH);
        }
        
        // Draw subtitle if provided
        if (subtitle != null && !subtitle.isEmpty()) {
            Font subtitleFont = new Font("Arial", Font.PLAIN, 28);
            int subtitleY = height - PADDING - 30;
            drawWrappedText(g2d, subtitle, subtitleFont, TEXT_COLOR, PADDING, subtitleY, TITLE_MAX_WIDTH);
        }
        
        // Add "READ MORE" button-like element
        Font readMoreFont = new Font("Arial", Font.BOLD, 24);
        g2d.setFont(readMoreFont);
        String readMoreText = "READ MORE";
        
        // Create button-like background
        FontMetrics readMoreMetrics = g2d.getFontMetrics(readMoreFont);
        int readMoreWidth = readMoreMetrics.stringWidth(readMoreText) + 40;
        int readMoreHeight = readMoreMetrics.getHeight() + 10;
        int readMoreX = width - readMoreWidth - PADDING;
        int readMoreY = height - readMoreHeight - PADDING;
        
        g2d.setColor(ACCENT_COLOR);
        g2d.fillRoundRect(readMoreX, readMoreY, readMoreWidth, readMoreHeight, 10, 10);
        
        // Draw READ MORE text
        g2d.setColor(TEXT_COLOR);
        g2d.drawString(readMoreText, readMoreX + 20, readMoreY + readMoreMetrics.getAscent() + 5);
        
        g2d.dispose();
        return resultImage;
    }

    @Override
    public BufferedImage downloadImage(String imageUrl) {
        try {
            log.info("Downloading image from URL: {}", imageUrl);
            
            // Add User-Agent header to avoid 403 errors
            URL url = new URL(imageUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");
            
            // For Pexels API, add the API key as a header if it's a Pexels URL
            if (imageUrl.contains("pexels.com")) {
                String pexelsApiKey = System.getenv("PEXELS_API_KEY");
                if (pexelsApiKey != null && !pexelsApiKey.isEmpty()) {
                    connection.setRequestProperty("Authorization", pexelsApiKey);
                }
            }
            
            try (InputStream in = connection.getInputStream()) {
                return ImageIO.read(in);
            }
        } catch (Exception e) {
            log.error("Error downloading image: {}", e.getMessage(), e);
            return createPlaceholderImage();
        }
    }
    
    private BufferedImage resizeImage(BufferedImage originalImage) {
        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();
        
        // If image is already square and correct size, return it
        if (originalWidth == TARGET_WIDTH && originalHeight == TARGET_HEIGHT) {
            return originalImage;
        }
        
        // Create a square image by cropping or padding
        BufferedImage resizedImage = new BufferedImage(TARGET_WIDTH, TARGET_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = resizedImage.createGraphics();
        
        // Fill background with black (for padding if needed)
        g2d.setColor(Color.BLACK);
        g2d.fillRect(0, 0, TARGET_WIDTH, TARGET_HEIGHT);
        
        // Calculate dimensions to maintain aspect ratio
        double scale;
        int x = 0, y = 0;
        int scaledWidth, scaledHeight;
        
        if (originalWidth > originalHeight) {
            // Landscape image
            scale = (double) TARGET_HEIGHT / originalHeight;
            scaledWidth = (int) (originalWidth * scale);
            scaledHeight = TARGET_HEIGHT;
            x = (TARGET_WIDTH - scaledWidth) / 2; // Center horizontally
        } else {
            // Portrait or square image
            scale = (double) TARGET_WIDTH / originalWidth;
            scaledWidth = TARGET_WIDTH;
            scaledHeight = (int) (originalHeight * scale);
            y = (TARGET_HEIGHT - scaledHeight) / 2; // Center vertically
        }
        
        // Draw the resized image
        g2d.drawImage(originalImage, x, y, scaledWidth, scaledHeight, null);
        g2d.dispose();
        
        return resizedImage;
    }
    
    private void drawWrappedText(Graphics2D g2d, String text, Font font, Color color, int x, int y, int maxWidth) {
        g2d.setFont(font);
        g2d.setColor(color);
        
        // Create attributed string for text wrapping
        AttributedString attributedText = new AttributedString(text);
        attributedText.addAttribute(TextAttribute.FONT, font);
        
        // Get font metrics
        FontRenderContext frc = g2d.getFontRenderContext();
        LineBreakMeasurer measurer = new LineBreakMeasurer(attributedText.getIterator(), frc);
        
        int currentY = y;
        measurer.setPosition(0);
        
        // Draw each line of text
        while (measurer.getPosition() < text.length()) {
            TextLayout layout = measurer.nextLayout(maxWidth);
            currentY += layout.getAscent();
            layout.draw(g2d, x, currentY);
            currentY += layout.getDescent() + layout.getLeading();
        }
    }
    
    private BufferedImage createPlaceholderImage() {
        BufferedImage placeholder = new BufferedImage(TARGET_WIDTH, TARGET_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = placeholder.createGraphics();
        
        // Fill with a gradient background
        GradientPaint gradient = new GradientPaint(0, 0, new Color(30, 30, 30), 
                                                  TARGET_WIDTH, TARGET_HEIGHT, new Color(60, 60, 60));
        g2d.setPaint(gradient);
        g2d.fillRect(0, 0, TARGET_WIDTH, TARGET_HEIGHT);
        
        // Add text
        g2d.setColor(Color.WHITE);
        Font font = new Font("Arial", Font.BOLD, 36);
        g2d.setFont(font);
        String text = "Image Not Available";
        
        FontMetrics metrics = g2d.getFontMetrics(font);
        int textX = (TARGET_WIDTH - metrics.stringWidth(text)) / 2;
        int textY = TARGET_HEIGHT / 2;
        
        g2d.drawString(text, textX, textY);
        g2d.dispose();
        
        return placeholder;
    }
} 