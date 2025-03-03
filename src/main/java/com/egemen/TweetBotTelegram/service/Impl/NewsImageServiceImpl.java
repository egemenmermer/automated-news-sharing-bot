package com.egemen.TweetBotTelegram.service.Impl;

import com.egemen.TweetBotTelegram.service.ImageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
public class NewsImageServiceImpl implements ImageService {

    @Override
    public void generateImageFromText(String text, OutputStream outputStream) throws Exception {
        // Create a news image with the text and write it to the output stream
        BufferedImage image = createNewsImage(text, text);
        ImageIO.write(image, "png", outputStream);
    }
    
    /**
     * Generates a news image with title and description text overlay
     * @param title The news title
     * @param description The news description
     * @return The path to the generated image file
     */
    public String generateNewsImage(String title, String description) {
        try {
            // Create a blank image with dimensions suitable for Instagram
            int width = 1080;  // Instagram recommended width
            int height = 1080; // Square format for Instagram
            
            BufferedImage image = createNewsImage(title, description);
            
            // Save the image
            String fileName = "generated-images/" + System.currentTimeMillis() + ".png";
            File outputFile = new File(fileName);
            outputFile.getParentFile().mkdirs();
            
            try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                ImageIO.write(image, "png", fos);
            }
            
            log.info("Generated news image: {}", fileName);
            return fileName;
        } catch (Exception e) {
            log.error("Failed to generate news image", e);
            return null;
        }
    }
    
    private BufferedImage createNewsImage(String title, String description) {
        int width = 1080;  // Instagram recommended width
        int height = 1080; // Square format for Instagram
        
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();

        // Set background color - dark gradient
        GradientPaint gradient = new GradientPaint(
            0, 0, new Color(33, 33, 33),
            0, height, new Color(66, 66, 66)
        );
        g2d.setPaint(gradient);
        g2d.fillRect(0, 0, width, height);

        // Add a subtle pattern or texture
        g2d.setColor(new Color(255, 255, 255, 20)); // Very transparent white
        for (int i = 0; i < height; i += 10) {
            g2d.drawLine(0, i, width, i);
        }

        // Configure text rendering
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Draw title
        g2d.setColor(Color.WHITE);
        Font titleFont = new Font("Arial", Font.BOLD, 48);
        g2d.setFont(titleFont);

        // Word wrap title
        String wrappedTitle = wrapText(title, g2d, width - 100);
        drawMultilineText(g2d, wrappedTitle, 50, 150);

        // Draw description
        Font descFont = new Font("Arial", Font.PLAIN, 32);
        g2d.setFont(descFont);
        String wrappedDesc = wrapText(description, g2d, width - 100);
        drawMultilineText(g2d, wrappedDesc, 50, 400);

        // Add timestamp
        Font timeFont = new Font("Arial", Font.ITALIC, 24);
        g2d.setFont(timeFont);
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        g2d.drawString(timestamp, 50, height - 50);

        // Add a border
        g2d.setColor(new Color(200, 200, 200, 100));
        g2d.setStroke(new BasicStroke(10));
        g2d.drawRect(20, 20, width - 40, height - 40);

        g2d.dispose();
        return image;
    }

    private String wrapText(String text, Graphics2D g2d, int maxWidth) {
        String[] words = text.split(" ");
        StringBuilder wrapped = new StringBuilder();
        StringBuilder line = new StringBuilder();

        for (String word : words) {
            if (line.length() > 0) {
                line.append(" ");
            }
            line.append(word);
            
            if (g2d.getFontMetrics().stringWidth(line.toString()) > maxWidth) {
                // If this isn't the first word on the line, remove it and put it on the next line
                if (line.length() > word.length()) {
                    line.delete(line.length() - word.length() - 1, line.length());
                    if (wrapped.length() > 0) {
                        wrapped.append("\n");
                    }
                    wrapped.append(line);
                    line = new StringBuilder(word);
                } else {
                    // If it's just one long word, keep it on this line
                    if (wrapped.length() > 0) {
                        wrapped.append("\n");
                    }
                    wrapped.append(line);
                    line = new StringBuilder();
                }
            }
        }

        if (line.length() > 0) {
            if (wrapped.length() > 0) {
                wrapped.append("\n");
            }
            wrapped.append(line);
        }

        return wrapped.toString();
    }

    private void drawMultilineText(Graphics2D g2d, String text, int x, int y) {
        int lineHeight = g2d.getFontMetrics().getHeight();
        String[] lines = text.split("\n");
        
        for (String line : lines) {
            g2d.drawString(line, x, y);
            y += lineHeight;
        }
    }
}