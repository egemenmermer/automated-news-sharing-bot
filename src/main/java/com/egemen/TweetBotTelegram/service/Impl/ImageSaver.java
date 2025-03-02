package com.egemen.TweetBotTelegram.service.Impl;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

@Service
public class ImageSaver {
    private static final String IMAGE_DIRECTORY = "src/main/resources/static/images";
    private static final Logger logger = LoggerFactory.getLogger(ImageSaver.class);
    private static int imageCounter = 1;

    private S3Client s3Client;
    private final String accessKey;
    private final String secretKey;
    private final String bucketName;
    private final String region;

    public ImageSaver(
            @Value("${AWS_ACCESS_KEY}") String awsAccessKey,
            @Value("${AWS_SECRET_KEY}") String awsSecretKey,
            @Value("${AWS_S3_BUCKET}") String awsS3Bucket,
            @Value("${AWS_REGION}") String awsRegion) {
        this.accessKey = awsAccessKey;
        this.secretKey = awsSecretKey;
        this.bucketName = awsS3Bucket;
        this.region = awsRegion;
    }

    @PostConstruct
    public void init() {
        this.s3Client = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)
                ))
                .build();
    }

    public String saveImageToFile(byte[] imageBytes) {
        if (imageBytes == null || imageBytes.length == 0) {
            throw new IllegalArgumentException("Image bytes cannot be null or empty");
        }

        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(imageBytes)) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy-HH:mm");
            String timestamp = dateFormat.format(new Date());
            String filename = timestamp + "-" + imageCounter++ + ".jpeg";

            // Local save
            File directory = new File(IMAGE_DIRECTORY);
            if (!directory.exists() && !directory.mkdirs()) {
                throw new IOException("Failed to create directory: " + IMAGE_DIRECTORY);
            }

            BufferedImage bufferedImage = ImageIO.read(inputStream);
            if (bufferedImage == null) {
                throw new IOException("Failed to read image data");
            }

            File localFile = new File(directory, filename);
            if (!ImageIO.write(bufferedImage, "jpeg", localFile)) {
                throw new IOException("Failed to write image file");
            }

            // Upload to S3
            String imageUrl = uploadToS3(imageBytes, filename);
            logger.info("Image uploaded successfully to S3: {}", imageUrl);

            // dosyayı pc den sil
            if (localFile.delete()) {
                logger.info("Local file deleted: {}", localFile.getAbsolutePath());
            } else {
                logger.warn("Failed to delete local file: {}", localFile.getAbsolutePath());
            }

            return imageUrl;

        } catch (IOException e) {
            logger.error("Failed to process image: ", e);
            throw new RuntimeException("Image processing failed", e);
        }
    }

    private String uploadToS3(byte[] fileBytes, String fileName) {
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileName)
                    .build();

            s3Client.putObject(request, RequestBody.fromBytes(fileBytes));

            return String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, fileName);
        } catch (Exception e) {
            logger.error("Failed to upload to S3: ", e);
            throw new RuntimeException("S3 upload failed", e);
        }
    }
}