package com.egemen.TweetBotTelegram.dto;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;



public class NewsResponseDTO {
    private List<NewsArticleDTO> articles;
    private String status;
    private int totalResults;

    public List<NewsArticleDTO> getArticles() {
        return articles;
    }

    public void setArticles(List<NewsArticleDTO> articles) {
        this.articles = articles;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getTotalResults() {
        return totalResults;
    }

    public void setTotalResults(int totalResults) {
        this.totalResults = totalResults;
    }
}
