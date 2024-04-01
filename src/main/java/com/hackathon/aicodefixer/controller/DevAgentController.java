package com.hackathon.aicodefixer.controller;

import com.hackathon.aicodefixer.model.ErrorRequest;
import com.hackathon.aicodefixer.service.ScmService;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@Slf4j
public class DevAgentController {

    @Value("${war.path}")
    String warPath;
    @Autowired
    ScmService scmService;

    @PostMapping("/v1/postErrors")
    public ResponseEntity postErrors(@RequestBody ErrorRequest error) throws GitAPIException, IOException {

        log.info("DevAgentController post errors");

            //Queue Mechanism - When P1 completes, then P2 ...
        scmService.getBinary(error);
     //   Resource warFile = new ClassPathResource("/Users/vsinsing/OraHacks2024/projectapp/build/libs/productapp.war");
        Resource file = new FileSystemResource(warPath);
        log.info("file {}",file.getFilename());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", "productapp.war");

        // Return the WAR file as the response entity
        return new ResponseEntity<>(file, headers, HttpStatus.OK);
        //return new ResponseEntity<>("success", HttpStatus.OK);
    }

    /*synchronised {

    }*/
}
