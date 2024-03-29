package com.hackathon.aicodefixer.service;

import com.hackathon.aicodefixer.model.ErrorRequest;
import com.hackathon.aicodefixer.model.LLMRequest;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service("scmService")
@Slf4j
public class ScmService {
    @Value("${github.repo.url}")
    private String repoURL;
    public void getBinary(ErrorRequest error) throws GitAPIException {
        log.info("getBinary STARTS");
        log.info("error {}", error.getMethodName());
        String downloadPath = "/Users/vsinsing/OraHacks2024";
       cloneGitRepo(repoURL,downloadPath);
        String codeSnippet = extractCodeSnippetfromMethod (downloadPath,error);
        processLLM(error.getException(),codeSnippet);
        log.info("getBinary ENDS");
    }

    private void processLLM(String exception, String codeSnippet) {

        try {
            RestTemplate restTemplate = new RestTemplate();

            String requestBody = exception.concat(codeSnippet);
            LLMRequest request = new LLMRequest();
            log.info("requestBody {}", requestBody);
            request.setText(requestBody);

            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Set request entity
            HttpEntity<LLMRequest> requestEntity = new HttpEntity<>(request, headers);

            // Make POST request
            log.info("Calling LLM API STARTS");
            ResponseEntity<String> responseEntity = restTemplate.exchange("http://phoenix431339.private5.oaceng02phx.oraclevcn.com:5001/getCodeFix", HttpMethod.POST, requestEntity, String.class);
            log.info("Calling LLM API ENDS");
            // Get response body
            String responseBody = responseEntity.getBody();

            log.info("responseBody {}", responseBody.toString());
            log.info("responseEntity.getStatusCode() {}", responseEntity.getStatusCode());
        }catch(Exception excp){
            log.error("LLM API call failed", excp);
        }
        //return responseBody;
    }

    private String extractCodeSnippetfromMethod(String downloadPath, ErrorRequest error) {
        String fileName = error.getClassName().concat(".java");
        log.info("Fetch fileName :"+ fileName);
        String methodName = error.getMethodName();
        log.info(" Method Name {}", methodName);
        File file = findFile(downloadPath,fileName);
        String methodSnippet = extractMethodContent(file.getAbsolutePath().toString(),methodName);
        return methodSnippet;
    }


    private void cloneGitRepo(String repositoryURL,String downloadPath) throws GitAPIException {
        log.info("cloneGitRepo:: repositoryURL {}, downloadPath {}",repositoryURL,downloadPath);
        try {
            log.info("Check if download Path contains git repo");
            deleteFolder(Path.of(downloadPath));
        } catch (IOException e) {
            log.error("git repo doesn't exists");
        }
        log.info("Cloning Starts");
        Git.cloneRepository()
                .setURI(repositoryURL)
                .setCredentialsProvider(new UsernamePasswordCredentialsProvider("vikashkumar805", "GitHub@OL2$"))
                .setDirectory(new File(downloadPath))
              .setBranch("main").call();
        log.info("Cloning Ends");
    }

    public static File findFile(String directoryPath, String fileName) {
        log.info("findFile STARTS");
        File directory = new File(directoryPath);

        if (!directory.exists()) {
            log.info("Directory does not exist.");
            return null;
        }

        File[] files = directory.listFiles();
        //log.info("files ::"+);
        if (files != null) {
            for (File file : files) {
                log.info("FileName {}", file.getName());
                if (file.isDirectory()) {
                    File foundFile = findFile(file.getAbsolutePath(), fileName);
                    if (foundFile != null) {
                        log.info("File located");
                        return foundFile;
                    }
                } else if (file.getName().equals(fileName)) {
                    return file;
                }
            }
        }
        log.info("file not found");
        log.info("findFile ENDS");
        return null;
    }

    public String extractMethodContent(String fileName, String methodName) {
        StringBuilder result = new StringBuilder();
        log.info("extractMethodContent STARTS");
        List<String> methodContent = new ArrayList<>();
        boolean inMethod = false;
        int openBracesCount = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains(methodName) && line.contains("{")) {
                    inMethod = true;
                    methodContent.add(line.trim());
                    openBracesCount++;
                    log.info("openBracesCount for {",openBracesCount );
                } else if (inMethod) {
                    methodContent.add(line.trim());
                    if (line.contains("{")) {
                        openBracesCount++;
                    }
                    if (line.contains("}")) {
                        openBracesCount--;
                        log.info("openBracesCount for }",openBracesCount );
                        if (openBracesCount == 0) {

                            break;
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        log.info("methodContent {}",methodContent);
        for(String str: methodContent){
            result.append(str);
        }
        return result.toString();
    }


    public void deleteFolder(Path dir) throws IOException {
        log.info("deleteFolder START");
       // path to the directory
        log.info("deleting repo from directory::" + dir);
        Files.walk(dir)
                .sorted(Comparator.reverseOrder())
                .forEach(
                        path -> {
                            try {
                                Files.delete(path);
                            } catch (IOException e) {
                                log.error("Unable to delete files::", e);
                            }
                        });
    }
}
