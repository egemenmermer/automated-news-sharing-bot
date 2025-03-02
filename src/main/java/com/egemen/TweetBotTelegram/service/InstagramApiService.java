package com.egemen.TweetBotTelegram.service;

import com.egemen.TweetBotTelegram.entity.InstagramPost;
import com.egemen.TweetBotTelegram.entity.News;

public interface InstagramApiService {
    boolean createPost(InstagramPost post);
    InstagramPost getPostById(Long id);
    void publishPost(InstagramPost post) throws Exception;
    void deletePost(String mediaId) throws Exception;
    String uploadImage(String imageUrl) throws Exception;
}