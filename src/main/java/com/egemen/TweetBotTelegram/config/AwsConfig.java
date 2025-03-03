package com.egemen.TweetBotTelegram.config;

import io.github.cdimascio.dotenv.Dotenv;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class AwsConfig {

    @Bean
    public String awsAccessKey() {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        String key = dotenv.get("AWS_ACCESS_KEY");
        log.info("Loaded AWS access key");
        return key;
    }
    
    @Bean
    public String awsSecretKey() {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        String key = dotenv.get("AWS_SECRET_KEY");
        log.info("Loaded AWS secret key");
        return key;
    }
    
    @Bean
    public String awsS3Bucket() {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        String bucket = dotenv.get("AWS_S3_BUCKET");
        log.info("Loaded AWS S3 bucket name");
        return bucket;
    }
    
    @Bean
    public String awsRegion() {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        String region = dotenv.get("AWS_REGION");
        log.info("Loaded AWS region");
        return region;
    }
} 