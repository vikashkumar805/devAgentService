package com.hackathon.aicodefixer.controller;

import com.hackathon.aicodefixer.model.ErrorRequest;
import com.hackathon.aicodefixer.service.ScmService;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
public class DevAgentController {

    @Autowired
    ScmService scmService;

    @PostMapping("/v1/postErrors")
    public ResponseEntity postErrors(@RequestBody ErrorRequest error) throws GitAPIException {

        log.info("DevAgentController post errors");

            //Queue Mechanism - When P1 completes, then P2 ...
       scmService.getBinary(error);
        return new ResponseEntity<>("success", HttpStatus.OK);
    }

    /*synchronised {

    }*/
}
