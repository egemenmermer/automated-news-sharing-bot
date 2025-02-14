package com.egemen.TweetBotTelegram.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PexelsResponseDTO {
    private int page;
    private int perPage;
    private List<PhotoDTO> photos;
    
    @JsonProperty("total_results")
    private int totalResults;


}