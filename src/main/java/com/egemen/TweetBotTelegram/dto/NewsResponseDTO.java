package com.egemen.TweetBotTelegram.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class NewsResponseDTO {

    @JsonProperty("data")
    private List<ArticleDTO> articles;

    public List<ArticleDTO> getArticles() {
        return articles;
    }
}