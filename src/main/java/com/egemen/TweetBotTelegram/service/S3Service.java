package com.egemen.TweetBotTelegram.service;

public interface S3Service {
    String uploadFile(byte[] fileBytes, String fileName, String contentType);
    String uploadFileFromUrl(String imageUrl);
    String uploadFileFromPath(String filePath);
} 