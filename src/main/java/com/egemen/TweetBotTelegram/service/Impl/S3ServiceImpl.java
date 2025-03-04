package com.egemen.TweetBotTelegram.service.Impl;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.egemen.TweetBotTelegram.service.S3Service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import com.amazonaws.HttpMethod;

import javax.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Service
public class S3ServiceImpl implements S3Service {

    private final String accessKey;
    private final String secretKey;
    private final String bucketName;
    private final String region;

    private AmazonS3 s3Client;
    private boolean s3Enabled = false;

    private final S3Client s3ClientSdk;

    @Autowired
    public S3ServiceImpl(String awsAccessKey, String awsSecretKey, String awsS3Bucket, String awsRegion) {
        this.accessKey = awsAccessKey;
        this.secretKey = awsSecretKey;
        this.bucketName = awsS3Bucket;
        this.region = awsRegion;
        log.info("S3ServiceImpl initialized with AWS credentials");
        initializeS3Client();
        this.s3ClientSdk = S3Client.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(awsAccessKey, awsSecretKey)
                ))
                .build();
    }

    private void initializeS3Client() {
        try {
            if (accessKey != null && !accessKey.isEmpty() && secretKey != null && !secretKey.isEmpty()) {
                AWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);
                s3Client = AmazonS3ClientBuilder.standard()
                        .withCredentials(new AWSStaticCredentialsProvider(credentials))
                        .withRegion(Regions.fromName(region))
                        .build();
                s3Enabled = true;
                log.info("S3 client initialized successfully");
            } else {
                log.warn("AWS credentials not provided. S3 functionality will be disabled.");
            }
        } catch (Exception e) {
            log.error("Failed to initialize S3 client: {}", e.getMessage());
        }
    }

    @Override
    public String uploadFile(byte[] fileBytes, String key, String contentType) {
        try {
            log.info("Uploading file to S3: {}", key);
            
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(contentType);
            metadata.setContentLength(fileBytes.length);

            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, key,
                new ByteArrayInputStream(fileBytes), metadata);
            s3Client.putObject(putObjectRequest);

            return s3Client.getUrl(bucketName, key).toString();
        } catch (Exception e) {
            log.error("Error uploading file to S3: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public String uploadFileAndGetPresignedUrl(byte[] fileBytes, String key, String contentType, int expirationInSeconds) {
        try {
            log.info("Uploading file to S3: {}", key);
            
            // Upload the file
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(contentType);
            metadata.setContentLength(fileBytes.length);

            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, key,
                new ByteArrayInputStream(fileBytes), metadata);
            s3Client.putObject(putObjectRequest);

            log.info("File uploaded successfully to S3: {}", key);

            // Generate presigned URL
            GeneratePresignedUrlRequest generatePresignedUrlRequest = new GeneratePresignedUrlRequest(bucketName, key)
                .withMethod(HttpMethod.GET)
                .withExpiration(Date.from(Instant.now().plusSeconds(expirationInSeconds)));

            URL url = s3Client.generatePresignedUrl(generatePresignedUrlRequest);
            log.info("Generated presigned URL: {}", url);
            
            return url.toString();

        } catch (Exception e) {
            log.error("Error uploading file to S3 and generating presigned URL: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public String uploadFileFromUrl(String imageUrl) {
        if (!s3Enabled) {
            log.warn("S3 is not enabled. Returning original URL or placeholder.");
            if (imageUrl != null && (imageUrl.startsWith("http://") || imageUrl.startsWith("https://"))) {
                return imageUrl; // Return the original URL if it's already a web URL
            }
            return "https://via.placeholder.com/1080x1080.png?text=Image";
        }
        
        try {
            URL url = new URL(imageUrl);
            byte[] imageBytes = url.openStream().readAllBytes();
            
            String fileName = imageUrl.substring(imageUrl.lastIndexOf('/') + 1);
            String contentType = "image/jpeg"; // Default to JPEG
            
            if (fileName.toLowerCase().endsWith(".png")) {
                contentType = "image/png";
            } else if (fileName.toLowerCase().endsWith(".gif")) {
                contentType = "image/gif";
            }
            
            return uploadFile(imageBytes, fileName, contentType);
        } catch (Exception e) {
            log.error("Error uploading file from URL to S3: {}", e.getMessage());
            return "https://via.placeholder.com/1080x1080.png?text=Error";
        }
    }

    @Override
    public String uploadFileFromPath(String filePath) {
        if (!s3Enabled) {
            log.warn("S3 is not enabled. Returning placeholder URL.");
            return "https://via.placeholder.com/1080x1080.png?text=LocalFile";
        }
        
        try {
            Path path = Paths.get(filePath);
            byte[] fileBytes = Files.readAllBytes(path);
            
            String fileName = path.getFileName().toString();
            String contentType = "image/jpeg"; // Default to JPEG
            
            if (fileName.toLowerCase().endsWith(".png")) {
                contentType = "image/png";
            } else if (fileName.toLowerCase().endsWith(".gif")) {
                contentType = "image/gif";
            }
            
            return uploadFile(fileBytes, fileName, contentType);
        } catch (Exception e) {
            log.error("Error uploading file from path to S3: {}", e.getMessage());
            return "https://via.placeholder.com/1080x1080.png?text=Error";
        }
    }
} 