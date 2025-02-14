package com.egemen.TweetBotTelegram.service.Impl;

import com.egemen.TweetBotTelegram.entity.InstagramPost;
import com.egemen.TweetBotTelegram.entity.News;
import com.egemen.TweetBotTelegram.entity.Bot;
import com.egemen.TweetBotTelegram.enums.PostStatus;
import com.egemen.TweetBotTelegram.repository.InstagramPostRepository;
import com.egemen.TweetBotTelegram.repository.NewsRepository;
import com.egemen.TweetBotTelegram.repository.BotRepository;
import com.egemen.TweetBotTelegram.service.InstagramService;
import com.egemen.TweetBotTelegram.service.ImageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class InstagramServiceImpl implements InstagramService {

    @Value("${INSTAGRAM_ACCESS_TOKEN}")
    private String accessToken;

    private final String INSTAGRAM_API_URL = "https://graph.instagram.com/v22.0";
    private final RestTemplate restTemplate;
    private final InstagramPostRepository instagramPostRepository;
    private final NewsRepository newsRepository;
    private final BotRepository botRepository;
    private final ImageService imageService;

    @Autowired
    public InstagramServiceImpl(
            InstagramPostRepository instagramPostRepository,
            NewsRepository newsRepository,
            BotRepository botRepository,
            ImageService imageService) {
        this.restTemplate = new RestTemplate();
        this.instagramPostRepository = instagramPostRepository;
        this.newsRepository = newsRepository;
        this.botRepository = botRepository;
        this.imageService = imageService;
    }

    @Override
    public List<InstagramPost> getAllPosts() {
        return instagramPostRepository.findAll();
    }

    @Override
    public List<InstagramPost> getPendingPosts() {
        return instagramPostRepository.findPendingPosts();
    }

    @Override
    public InstagramPost getPost(Long id) {
        return instagramPostRepository.findById(id).orElse(null);
    }

    @Override
    public InstagramPost createPost(Long newsId, String imageUrl) {
        Optional<Bot> defaultBot = botRepository.findAll().stream().findFirst();
        if (defaultBot.isEmpty()) {
            log.error("No bot found in the system");
            return null;
        }
        return createPost(newsId, imageUrl, defaultBot.get().getId());
    }

    @Override
    public void createPost(String title, String caption, String imageUrl) {
        try {
            Optional<Bot> defaultBot = botRepository.findAll().stream().findFirst();
            if (defaultBot.isEmpty()) {
                log.error("No bot found in the system");
                return;
            }

            if (!imageService.isValidForInstagram(imageUrl)) {
                log.error("Image does not meet Instagram requirements: {}", imageUrl);
                return;
            }

            String processedImageUrl = imageService.processImageForInstagram(imageUrl);
            if (processedImageUrl == null) {
                log.error("Failed to process image for Instagram");
                return;
            }

            String mediaContainerId = createMediaContainer(processedImageUrl, 
                formatCaption(title, caption));
            if (mediaContainerId == null) {
                log.error("Failed to create media container");
                return;
            }

            InstagramPost post = new InstagramPost();
            post.setBot(defaultBot.get());
            post.setImageUrl(processedImageUrl);
            post.setCaption(formatCaption(title, caption));
            post.setInstagramPostId(mediaContainerId);
            post.setStatus(PostStatus.PENDING);
            post.setRetryCount(0);
            post.setCreatedAt(LocalDateTime.now());

            instagramPostRepository.save(post);
            log.info("Successfully created Instagram post with container ID: {}", mediaContainerId);
        } catch (Exception e) {
            log.error("Error creating direct Instagram post: {}", e.getMessage());
        }
    }

    public InstagramPost createPost(Long newsId, String imageUrl, Long botId) {
        try {
            Optional<News> newsOptional = newsRepository.findById(newsId);
            Optional<Bot> botOptional = botRepository.findById(botId);
            
            if (newsOptional.isEmpty()) {
                log.error("News not found with id: {}", newsId);
                return null;
            }
            if (botOptional.isEmpty()) {
                log.error("Bot not found with id: {}", botId);
                return null;
            }

            News news = newsOptional.get();
            Bot bot = botOptional.get();

            if (!imageService.isValidForInstagram(imageUrl)) {
                log.error("Image does not meet Instagram requirements: {}", imageUrl);
                String searchQuery = imageService.createSearchQuery(news.getTitle(), news.getContent());
                String newImageUrl = imageService.searchPexelsImage(searchQuery);
                if (newImageUrl == null || !imageService.isValidForInstagram(newImageUrl)) {
                    log.error("Could not find valid replacement image");
                    return null;
                }
                imageUrl = newImageUrl;
            }

            String processedImageUrl = imageService.processImageForInstagram(imageUrl);
            if (processedImageUrl == null) {
                log.error("Failed to process image for Instagram");
                return null;
            }

            String caption = formatCaption(news.getTitle(), news.getSummary());
            String mediaContainerId = createMediaContainer(processedImageUrl, caption);
            if (mediaContainerId == null) {
                log.error("Failed to create media container");
                return null;
            }

            InstagramPost post = new InstagramPost();
            post.setNews(news);
            post.setBot(bot);
            post.setImageUrl(processedImageUrl);
            post.setCaption(caption);
            post.setInstagramPostId(mediaContainerId);
            post.setStatus(PostStatus.PENDING);
            post.setRetryCount(0);

            return instagramPostRepository.save(post);
        } catch (Exception e) {
            log.error("Error creating Instagram post: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public InstagramPost publishPost(Long id) {
        Optional<InstagramPost> postOptional = instagramPostRepository.findById(id);
        if (postOptional.isEmpty()) {
            log.error("Post not found with id: {}", id);
            return null;
        }

        InstagramPost post = postOptional.get();
        try {
            publishMedia(post.getInstagramPostId());
            post.setStatus(PostStatus.POSTED);
            post.setPostedAt(LocalDateTime.now());
            return instagramPostRepository.save(post);
        } catch (Exception e) {
            post.setStatus(PostStatus.FAILED);
            post.incrementRetryCount();
            instagramPostRepository.save(post);
            log.error("Error publishing post: {}", e.getMessage());
            return null;
        }
    }

    private void publishMedia(String mediaContainerId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            
            String url = INSTAGRAM_API_URL + "/media_publish";
            
            Map<String, String> body = Map.of("creation_id", mediaContainerId);

            HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);
            restTemplate.postForEntity(url, request, Map.class);
            
            log.info("Successfully published media with container ID: {}", mediaContainerId);
        } catch (Exception e) {
            log.error("Error publishing media: {}", e.getMessage());
            throw e;
        }
    }

    @Override
    public void publishPendingPosts() {
        List<InstagramPost> pendingPosts = instagramPostRepository.findPendingPosts();
        for (InstagramPost post : pendingPosts) {
            publishPost(post.getId());
        }
        
        List<InstagramPost> failedPosts = instagramPostRepository.findFailedRetryablePosts(3);
        for (InstagramPost post : failedPosts) {
            publishPost(post.getId());
        }
    }

    @Override
    public void deletePost(Long id) {
        try {
            instagramPostRepository.deleteById(id);
            log.info("Successfully deleted post with id: {}", id);
        } catch (Exception e) {
            log.error("Error deleting post with id {}: {}", id, e.getMessage());
        }
    }

    private String formatCaption(String title, String summary) {
        return String.format("""
                📰 %s
                
                %s
                
                #news #updates #dailynews #newsupdate #technews #technology
                """, title, summary);
    }

    private String createMediaContainer(String imageUrl, String caption) {
        try {
            if (!imageService.validateImage(imageUrl, 320, 320)) {
                log.error("Image dimensions do not meet Instagram requirements");
                return null;
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            String url = INSTAGRAM_API_URL + "/media";
            
            Map<String, String> body = Map.of(
                "image_url", imageUrl,
                "caption", caption
            );

            HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return response.getBody().get("id").toString();
            }
        } catch (Exception e) {
            log.error("Error creating media container: {}", e.getMessage());
        }
        return null;
    }
}
