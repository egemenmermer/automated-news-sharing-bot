package com.egemen.TweetBotTelegram.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NewsResponseDTO {
    private List<NewsArticleDTO> data;
    private Pagination pagination;

    // Getters and Setters
    public List<NewsArticleDTO> getData() {
        return data;
    }

    public void setData(List<NewsArticleDTO> data) {
        this.data = data;
    }

    public Pagination getPagination() {
        return pagination;
    }

    public void setPagination(Pagination pagination) {
        this.pagination = pagination;
    }

    public static class Pagination {
        private int limit;
        private int offset;
        private int count;
        private int total;


    }
}
