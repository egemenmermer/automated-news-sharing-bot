package com.egemen.TweetBotTelegram.service;

import com.egemen.TweetBotTelegram.entity.News;

import java.io.OutputStream;

public interface ImageService {
    void generateImageFromText(String text, OutputStream outputStream) throws Exception;
}