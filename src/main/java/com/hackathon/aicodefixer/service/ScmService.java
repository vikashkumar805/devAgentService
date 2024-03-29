package com.hackathon.aicodefixer.service;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;

@Service("scmService")
@Slf4j
public class ScmService {
    @Value("${github.repo.url}")
    private String repoURL;
    public void getBinary(String error) throws GitAPIException {
        log.info("error {}", error);
        String downloadPath = "/Users/vsinsing/fawOpsDashboard/OraHacks2024";
        cloneGitRepo(repoURL,downloadPath );
    }

    private void cloneGitRepo(String repositoryURL,String downloadPath) throws GitAPIException {
        Git.cloneRepository()
                .setURI(repositoryURL)
                .setCredentialsProvider(new UsernamePasswordCredentialsProvider("vikashkumar805", "GitHub@OL2$"))
                .setDirectory(new File(downloadPath))
              .setBranch("main").call();
    }
}
