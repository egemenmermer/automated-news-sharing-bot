package com.egemen.TweetBotTelegram.service.Impl;

import com.egemen.TweetBotTelegram.service.ImageProcessingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.apache.commons.lang3.StringUtils;

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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class ImageProcessingServiceImpl implements ImageProcessingService {

    private static final int TARGET_WIDTH = 1080;
    private static final int TARGET_HEIGHT = 1080;
    private static final Color OVERLAY_COLOR = new Color(0, 0, 0, 160);
    private static final Color TEXT_COLOR = Color.WHITE;
    private static final Color ACCENT_COLOR = new Color(255, 45, 45);
    private static final String LOGO_TEXT = "NEURAL NEWS";
    private static final int PADDING = 50;
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
    public BufferedImage addTextToImage(BufferedImage sourceImage, String title, String content) {
        // Add null checks for title and content
        if (title == null || title.trim().isEmpty()) {
            title = "No title available";
        }
        if (content == null || content.trim().isEmpty()) {
            content = "No content available";
        }

        int width = sourceImage.getWidth();
        int height = sourceImage.getHeight();
        
        // Create a new image
        BufferedImage resultImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = resultImage.createGraphics();
        
        // Enable anti-aliasing
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        
        // Draw original image
        g2d.drawImage(sourceImage, 0, 0, null);
        
        // Add semi-transparent overlay
        g2d.setColor(new Color(0, 0, 0, 180));
        g2d.fillRect(0, 0, width, height);
        
        // Draw logo
        Font logoFont = new Font("Arial", Font.BOLD, 48);
        g2d.setFont(logoFont);
        g2d.setColor(ACCENT_COLOR);
        drawCenteredString(g2d, LOGO_TEXT, width / 2, PADDING + 48);
        
        // Draw title
        Font titleFont = new Font("Arial", Font.BOLD, 36);
        g2d.setFont(titleFont);
        g2d.setColor(TEXT_COLOR);
        int titleY = height / 4;
        drawWrappedText(g2d, title, titleFont, TEXT_COLOR, PADDING, titleY, width - (PADDING * 2));
        
        // Draw content
        if (content != null && !content.isEmpty()) {
            Font contentFont = new Font("Arial", Font.PLAIN, 28);
            g2d.setFont(contentFont);
            int contentY = height / 2;
            drawWrappedText(g2d, content, contentFont, TEXT_COLOR, PADDING, contentY, width - (PADDING * 2));
        }
        
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
        // Add null check for text
        if (text == null || text.trim().isEmpty()) {
            log.warn("Received null or empty text for drawing");
            text = "No content available";
        }

        try {
            // Create attributed string
            AttributedString attributedText = new AttributedString(text);
            attributedText.addAttribute(TextAttribute.FONT, font);
            attributedText.addAttribute(TextAttribute.FOREGROUND, color);

            // Get line break measurer
            FontRenderContext frc = g2d.getFontRenderContext();
            LineBreakMeasurer measurer = new LineBreakMeasurer(
                attributedText.getIterator(),
                frc
            );

            // Set position
            int currentY = y;
            measurer.setPosition(0);

            // Draw each line
            while (measurer.getPosition() < text.length()) {
                TextLayout layout = measurer.nextLayout(maxWidth);
                currentY += layout.getAscent();
                float dx = x + (maxWidth - layout.getAdvance()) / 2; // Center the line
                layout.draw(g2d, dx, currentY);
                currentY += layout.getDescent() + layout.getLeading();
            }
        } catch (Exception e) {
            log.error("Error drawing wrapped text: {}", e.getMessage());
            // Draw simple text as fallback
            g2d.setFont(font);
            g2d.setColor(color);
            g2d.drawString(text, x, y + g2d.getFontMetrics().getAscent());
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

    public BufferedImage createImageWithText(File imageFile, String text, Font font, Color color, boolean centered) {
        try {
            BufferedImage image = ImageIO.read(imageFile);
            
            // Create a semi-transparent overlay
            BufferedImage overlay = new BufferedImage(
                image.getWidth(), 
                image.getHeight(), 
                BufferedImage.TYPE_INT_ARGB
            );
            
            Graphics2D g2d = overlay.createGraphics();
            g2d.setRenderingHint(
                RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON
            );
            
            // Add semi-transparent black background
            g2d.setColor(new Color(0, 0, 0, 180));
            g2d.fillRect(0, 0, image.getWidth(), image.getHeight());
            
            // Configure text drawing
            g2d.setFont(font);
            g2d.setColor(color);
            
            // Calculate text layout
            FontMetrics metrics = g2d.getFontMetrics(font);
            String[] lines = wrapText(text, metrics, image.getWidth() - 60);
            
            int lineHeight = metrics.getHeight();
            int totalTextHeight = lineHeight * lines.length;
            
            // Calculate starting Y position for centered text
            int y;
            if (centered) {
                y = (image.getHeight() - totalTextHeight) / 2;
            } else {
                y = image.getHeight() - totalTextHeight - 40; // Original bottom position
            }
            
            // Draw each line
            for (String line : lines) {
                int x = (image.getWidth() - metrics.stringWidth(line)) / 2; // Center horizontally
                g2d.drawString(line, x, y);
                y += lineHeight;
            }
            
            g2d.dispose();
            
            // Combine original image with overlay
            Graphics2D imageG2d = image.createGraphics();
            imageG2d.drawImage(overlay, 0, 0, null);
            imageG2d.dispose();
            
            return image;
            
        } catch (IOException e) {
            log.error("Error creating image with text: {}", e.getMessage());
            throw new RuntimeException("Failed to create image with text", e);
        }
    }
    
    private String[] wrapText(String text, FontMetrics metrics, int maxWidth) {
        String[] words = text.split("\\s+");
        List<String> lines = new ArrayList<>();
        StringBuilder currentLine = new StringBuilder();
        
        for (String word : words) {
            if (currentLine.length() == 0) {
                currentLine.append(word);
            } else {
                String testLine = currentLine + " " + word;
                if (metrics.stringWidth(testLine) <= maxWidth) {
                    currentLine.append(" ").append(word);
                } else {
                    lines.add(currentLine.toString());
                    currentLine = new StringBuilder(word);
                }
            }
        }
        
        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }
        
        return lines.toArray(new String[0]);
    }

    /**
     * Draws a string centered at the specified coordinates
     */
    private void drawCenteredString(Graphics2D g2d, String text, int centerX, int centerY) {
        FontMetrics metrics = g2d.getFontMetrics();
        int x = centerX - (metrics.stringWidth(text) / 2);
        int y = centerY - (metrics.getHeight() / 2) + metrics.getAscent();
        g2d.drawString(text, x, y);
    }
} 