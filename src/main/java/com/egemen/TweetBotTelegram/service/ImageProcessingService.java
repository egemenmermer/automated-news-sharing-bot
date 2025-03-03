package com.egemen.TweetBotTelegram.service;

import java.awt.image.BufferedImage;
import java.io.File;

public interface ImageProcessingService {
    
    /**
     * Adds text overlay to an image
     * 
     * @param imageUrl URL of the source image
     * @param title Main title text to overlay
     * @param subtitle Optional subtitle text
     * @return File object of the processed image
     */
    File createNewsImageWithText(String imageUrl, String title, String subtitle);
    
    /**
     * Adds text overlay to an image
     * 
     * @param sourceImage BufferedImage to process
     * @param title Main title text to overlay
     * @param subtitle Optional subtitle text
     * @return BufferedImage with text overlay
     */
    BufferedImage addTextToImage(BufferedImage sourceImage, String title, String subtitle);
    
    /**
     * Downloads an image from a URL
     * 
     * @param imageUrl URL of the image to download
     * @return BufferedImage object
     */
    BufferedImage downloadImage(String imageUrl);
} 