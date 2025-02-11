package com.egemen.TweetBotTelegram.service.Impl;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.egemen.TweetBotTelegram.service.S3Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.UUID;

@Service
public class S3ServiceImpl implements S3Service {
    private static final Logger log = LoggerFactory.getLogger(S3ServiceImpl.class);

    private final AmazonS3 amazonS3;
    
    @Value("${aws.s3.bucket}")
    private String bucketName;

    public S3ServiceImpl(AmazonS3 amazonS3) {
        this.amazonS3 = amazonS3;
    }

    @Override
    public String uploadImage(InputStream inputStream, String fileName) {
        try {
            String uniqueFileName = generateUniqueFileName(fileName);
            ObjectMetadata metadata = new ObjectMetadata();
            
            PutObjectRequest request = new PutObjectRequest(
                bucketName,
                uniqueFileName,
                inputStream,
                metadata
            );
            
            amazonS3.putObject(request);
            return amazonS3.getUrl(bucketName, uniqueFileName).toString();
        } catch (Exception e) {
            log.error("Error uploading file to S3: {}", e.getMessage());
            throw new RuntimeException("Failed to upload file to S3", e);
        }
    }

    @Override
    public void deleteImage(String fileName) {
        try {
            log.info("Deleting image from S3: {}", fileName);
            amazonS3.deleteObject(bucketName, fileName);
        } catch (Exception e) {
            log.error("Error deleting image from S3: {}", e.getMessage());
            throw new RuntimeException("Failed to delete image from S3", e);
        }
    }

    @Override
    public String getImageUrl(String fileName) {
        try {
            if (!amazonS3.doesObjectExist(bucketName, fileName)) {
                log.error("Image not found in S3: {}", fileName);
                return null;
            }
            return amazonS3.getUrl(bucketName, fileName).toString();
        } catch (Exception e) {
            log.error("Error getting image URL from S3: {}", e.getMessage());
            throw new RuntimeException("Failed to get image URL from S3", e);
        }
    }

    @Override
    public InputStream getImage(String fileName) {
        try {
            log.info("Fetching image from S3: {}", fileName);
            return amazonS3.getObject(bucketName, fileName).getObjectContent();
        } catch (Exception e) {
            log.error("Error fetching image from S3: {}", e.getMessage());
            throw new RuntimeException("Failed to fetch image from S3", e);
        }
    }

    private String generateUniqueFileName(String originalFileName) {
        String extension = "";
        int lastDot = originalFileName.lastIndexOf('.');
        if (lastDot > 0) {
            extension = originalFileName.substring(lastDot);
        }
        return UUID.randomUUID().toString() + extension;
    }
}