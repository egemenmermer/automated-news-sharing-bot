package com.egemen.TweetBotTelegram.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PhotoDTO {
    private int id;
    private int width;
    private int height;
    private String url;
    private String photographer;
    @JsonProperty("src")
    private PhotoSourceDTO source;

}
