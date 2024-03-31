package com.hackathon.aicodefixer.model;

import lombok.Data;

@Data
public class GitPRRequest {

    private String title;
    private String head;
    private String body;
    private String base;
}
