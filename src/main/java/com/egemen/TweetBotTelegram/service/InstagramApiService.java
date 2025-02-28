package com.egemen.TweetBotTelegram.service;

import com.egemen.TweetBotTelegram.entity.Bot;
import com.egemen.TweetBotTelegram.entity.Tweet;
import com.egemen.TweetBotTelegram.exception.InstagramApiException;

public interface InstagramApiService {
    void processAndPostToInstagram(int botId);
    void createPostLog(Bot bot, int postCount, Tweet tweet);
    void handlePostingError(Tweet tweet, Exception e);
    String uploadImageToInstagram(String imageUrl,String caption);
    boolean publishPost(String mediaId);
    void checkAndRefreshToken();
}