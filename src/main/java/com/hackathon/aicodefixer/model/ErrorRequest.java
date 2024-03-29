package com.hackathon.aicodefixer.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonInclude
public class ErrorRequest {

    @JsonProperty("error")
    private String error;
}
