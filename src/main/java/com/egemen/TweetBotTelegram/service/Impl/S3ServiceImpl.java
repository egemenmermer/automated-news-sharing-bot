package com.egemen.TweetBotTelegram.service.Impl;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.CannedAccessControlList;
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

import javax.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
    public String uploadFile(byte[] fileBytes, String fileName, String contentType) {
        try {
            log.info("Uploading file to S3: {}", fileName);
            
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(fileBytes.length);
            metadata.setContentType(contentType);
            
            InputStream inputStream = new ByteArrayInputStream(fileBytes);
            
            // Create a PutObjectRequest without ACL
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, fileName, inputStream, metadata);
            
            // Upload the file
            s3Client.putObject(putObjectRequest);
            
            // Generate the URL
            String fileUrl = s3Client.getUrl(bucketName, fileName).toString();
            log.info("File uploaded successfully to S3: {}", fileUrl);
            
            return fileUrl;
        } catch (Exception e) {
            log.error("Error uploading file to S3: {}", e.getMessage());
            // Return a fallback URL
            return "https://images.pexels.com/photos/3184360/pexels-photo-3184360.jpeg";
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