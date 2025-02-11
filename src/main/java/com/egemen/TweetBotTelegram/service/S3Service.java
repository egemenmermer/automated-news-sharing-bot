package com.egemen.TweetBotTelegram.service;

import java.io.InputStream;

public interface S3Service {
    /**
     * Uploads an image to S3 and returns its public URL
     * @param inputStream The image data as InputStream
     * @param fileName Optional filename (will generate if null)
     * @return Public URL of the uploaded image
     */
    String uploadImage(InputStream inputStream, String fileName);

    /**
     * Deletes an image from S3
     * @param fileName Name of the file to delete
     */
    void deleteImage(String fileName);

    /**
     * Gets the public URL of an existing image
     * @param fileName Name of the file
     * @return Public URL of the image, or null if not found
     */
    String getImageUrl(String fileName);

    /**
     * Gets the image data as InputStream
     * @param fileName Name of the file
     * @return InputStream of the image data
     */
    InputStream getImage(String fileName);
}