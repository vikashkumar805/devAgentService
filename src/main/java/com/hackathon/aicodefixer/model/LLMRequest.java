package com.hackathon.aicodefixer.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class LLMRequest {

    @JsonProperty("text")
    private String text;
}
