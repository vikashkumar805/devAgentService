package com.hackathon.aicodefixer.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonInclude
public class ErrorRequest {

    @JsonProperty("exception")
    private String exception;

    /*@JsonProperty("scm_exception_details")
    private String scm_exception_details;*/

    @JsonProperty("className")
    private String className;

    @JsonProperty("methodName")
    private String methodName;

    @JsonProperty("lineNo")
    private String lineNo;

    @JsonProperty("issueId")
    private String issueId;
}
