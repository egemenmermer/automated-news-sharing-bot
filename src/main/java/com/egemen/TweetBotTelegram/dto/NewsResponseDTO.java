package com.egemen.TweetBotTelegram.dto;

import java.util.List;

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

        // Getters and Setters
        public int getLimit() {
            return limit;
        }

        public void setLimit(int limit) {
            this.limit = limit;
        }

        public int getOffset() {
            return offset;
        }

        public void setOffset(int offset) {
            this.offset = offset;
        }

        public int getCount() {
            return count;
        }

        public void setCount(int count) {
            this.count = count;
        }

        public int getTotal() {
            return total;
        }

        public void setTotal(int total) {
            this.total = total;
        }
    }
}
