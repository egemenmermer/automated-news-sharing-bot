package com.egemen.TweetBotTelegram.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PhotoSourceDTO {
    private String original;
    private String large;
    private String medium;
    private String small;
}
