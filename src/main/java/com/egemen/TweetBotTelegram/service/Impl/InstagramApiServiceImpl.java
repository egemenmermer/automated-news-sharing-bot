package com.egemen.TweetBotTelegram.service.Impl;

import com.egemen.TweetBotTelegram.entity.Bot;
import com.egemen.TweetBotTelegram.entity.News;
import com.egemen.TweetBotTelegram.entity.PostLogs;
import com.egemen.TweetBotTelegram.entity.Tweet;
import com.egemen.TweetBotTelegram.enums.PostStatus;
import com.egemen.TweetBotTelegram.enums.TweetStatus;
import com.egemen.TweetBotTelegram.exception.InstagramApiException;
import com.egemen.TweetBotTelegram.repository.BotRepository;
import com.egemen.TweetBotTelegram.repository.PostLogsRepository;
import com.egemen.TweetBotTelegram.repository.TweetsRepository;
import com.egemen.TweetBotTelegram.service.ImageService;
import com.egemen.TweetBotTelegram.service.InstagramApiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class InstagramApiServiceImpl implements InstagramApiService {

    private static final Logger logger = LoggerFactory.getLogger(InstagramApiServiceImpl.class);
    private static final int MAX_CAPTION_LENGTH = 2200;

    @Value("${instagram.client.id}")
    private String clientId;

    @Value("${instagram.client.secret}")
    private String clientSecret;

    private String accessToken="IGAAXmZBgTZAeeNBZAE9ON2xrbW80dnNTalk5T1BocThsV0VLUGJ3NHV3ZAFFFcFlaNnQ3ZAEt5TXV0MWc1TlhVXzRSYmF0Ym1tZAmZAxTUJKVGN5eUJGd3c0WFpaZAndHdlloN3dtMWQ2V1doaFVxajlvU2JENk5Uc2JZANWN2bVZAKeVRGVQZDZD";

    private LocalDateTime startTime = LocalDateTime.now();

    @Autowired
    private TweetsRepository tweetsRepository;

    @Autowired
    private PostLogsRepository postLogsRepository;

    @Autowired
    private ImageService imageService;

    @Autowired
    private ImageSaver imageSaver;

    @Autowired
    private BotRepository botsRepository;

    @Value("${instagram.user.id}")
    private String instagramUserId;
    private final RestTemplate restTemplate = new RestTemplate();

    static int postCount = 1;

    @Override
    public void processAndPostToInstagram(int botId) {
        checkAndRefreshToken();
        Bot bot = botsRepository.findById((long) botId)
                .orElseThrow(() -> new RuntimeException("Bot not found"));
        List<Tweet> generatedTweets = tweetsRepository.findByStatusAndBot(TweetStatus.GENERATED, bot);

        if(generatedTweets.isEmpty()){
            logger.info("No generated tweets found to process");
            return;
        }


        for (Tweet tweet : generatedTweets) {
            try {
                ByteArrayOutputStream imageStream = new ByteArrayOutputStream();
                imageService.generateImageFromText(tweet.getContent(), imageStream);
                byte[] imageBytes = imageStream.toByteArray();
                String imageUrl = imageSaver.saveImageToFile(imageBytes); // Dosya kaydet
                Optional<News> news = tweetsRepository.findNewsByTweetId(tweet.getId());
                if(news.isEmpty()){
                    logger.error("News not found for tweet ID: {}", tweet.getId());
                    continue;
                }

                // Instagram'a yükle
                String mediaId = uploadImageToInstagram(imageUrl, formatCaption(news.get().getTitle()) + ": " + formatCaption(news.get().getContent()));
                if (mediaId == null) {
                    logger.error("Failed to upload image for tweet ID: {}", tweet.getId());
                    continue;
                }else{
                    logger.info("Successfully uploaded image for tweet ID: {}", tweet.getId());
                }


                // Gönderiyi paylaş
                boolean posted = publishPost(mediaId);

                if (posted) {
                    tweet.setStatus(TweetStatus.POSTED);
                    tweetsRepository.save(tweet);
                    createPostLog(bot, postCount, tweet);
                    postCount++;
                    logger.info("Successfully posted to Instagram, Tweet ID: {}", tweet.getId());
                }else {
                    logger.error("Failed to post to Instagram, Tweet ID: {}", tweet.getId());
                }

                Thread.sleep(1000);

            } catch (Exception e) {
                logger.error("Error posting to Instagram, Tweet ID {}: {}", tweet.getId(), e.getMessage());
                handlePostingError(tweet, e);
            }
        }
    }




    @Override
    public String uploadImageToInstagram(String imageUrl, String caption) throws InstagramApiException {
        String url = "https://graph.instagram.com/v22.0/me/media";


        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("image_url", imageUrl);
        requestBody.put("caption", caption);

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, requestEntity, Map.class);

            if (response.getBody() != null && response.getBody().containsKey("id")) {
                // Doğrudan String olarak al
                return (String) response.getBody().get("id");
            }
            throw new InstagramApiException("Failed to upload image");
        } catch (HttpClientErrorException e) {
            logger.error("Response body: " + e.getResponseBodyAsString());
            throw new InstagramApiException("Instagram API error: " + e.getMessage());
        }
    }
    @Override
    public boolean publishPost(String mediaId) {
        String url = "https://graph.instagram.com/v22.0/" + instagramUserId + "/media_publish";

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("creation_id", new BigInteger(mediaId));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, requestEntity, Map.class);
            return response.getStatusCode() == HttpStatus.OK;
        } catch (HttpClientErrorException e) {
            logger.error("Instagram API error: {}", e.getMessage());
            logger.error("Response body: {}", e.getResponseBodyAsString());
            return false;
        }
    }

    private String formatCaption(String caption) {
        if (caption.length() > MAX_CAPTION_LENGTH) {
            return caption.substring(0, MAX_CAPTION_LENGTH - 3) + "...";
        }
        return caption;
    }

    public void handlePostingError(Tweet tweet, Exception e) {
        tweet.setRetryCount(tweet.getRetryCount() - 1);
        if (tweet.getRetryCount() <= 0) {
            tweet.setStatus(TweetStatus.FAILED);
        } else {
            tweet.setStatus(TweetStatus.IN_PROGRESS);
            tweet.setScheduledAt(Timestamp.valueOf(LocalDateTime.now().plusMinutes(30)));
        }
        tweetsRepository.save(tweet);
    }

    public void createPostLog(Bot bot, int postCount, Tweet tweet) {
        PostLogs postLog = new PostLogs();
        postLog.setBot(bot);
        postLog.setPostedTweet(tweet);
        postLog.setPostStatus(PostStatus.SUCCESS);
        postLog.setPostCount(postCount);
        postLog.setPostedAt(Timestamp.valueOf(LocalDateTime.now()));
        postLogsRepository.save(postLog);
    }

    public void refreshAccessToken() {
        String url = "https://graph.instagram.com/refresh_access_token?grant_type=ig_refresh_token&access_token=" + accessToken;
        // API isteği yapılacak, yeni token alınacak ve accessToken değişkenine atılacak
        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, null, Map.class);
        if (response.getBody() != null && response.getBody().containsKey("access_token")) {
            accessToken = (String) response.getBody().get("access_token");
            logger.info("Token successfully refreshed");
        }
    }
    @Scheduled(cron = "0 0 0 1/30 * *")
    public void checkAndRefreshToken() {
        if (ChronoUnit.DAYS.between(startTime, LocalDateTime.now()) >= 30) {
            refreshAccessToken();
            startTime = LocalDateTime.now();
        }
    }
}