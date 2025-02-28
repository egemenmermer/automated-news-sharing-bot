package com.egemen.TweetBotTelegram.exception;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DetailedError {
    private String message;
    private String hostName;
    private String path;
    private Date createTime;
}