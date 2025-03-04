package com.egemen.TweetBotTelegram.service;

public interface S3Service {
    String uploadFile(byte[] fileBytes, String key, String contentType);
    String uploadFileAndGetPresignedUrl(byte[] fileBytes, String key, String contentType, int expirationInSeconds);
    String uploadFileFromUrl(String imageUrl);
    String uploadFileFromPath(String filePath);
} 