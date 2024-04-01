package com.hackathon.aicodefixer.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hackathon.aicodefixer.model.*;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.nio.charset.StandardCharsets;
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

    @Value("${downloadPath}")
    private String downloadPath;

    @Value("${git.user.name}")
    private String username;

    public void getBinary(ErrorRequest error) throws GitAPIException, IOException {
        log.info("getBinary STARTS");
        log.info("error {}", error.getMethodName());
        //String downloadPath = "/Users/vsinsing/OraHacks2024";
        String branchName = error.getIssueId();
        cloneGitRepo(repoURL,downloadPath, branchName,error);


        log.info("getBinary ENDS");
    }

    private String processLLM(String exception, String codeSnippet) throws JsonProcessingException {

        String responseBody = "";
        LLMResponse llmresponse = new LLMResponse();
        try {
            RestTemplate restTemplate = new RestTemplate();

            log.info("exception {}", exception);
            log.info("codesnippet {}", codeSnippet);
            String requestBody = createRequestBody(exception, codeSnippet);
           // String requestBody = exception.concat(codeSnippet);
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
             responseBody = responseEntity.getBody();

            log.info("responseBody {}", responseBody);
            log.info("responseEntity.getStatusCode() {}", responseEntity.getStatusCode());
            ObjectMapper objectMapper = new ObjectMapper();
             llmresponse = objectMapper.readValue(responseBody, LLMResponse.class);
            log.info("llmresponse {}",llmresponse);
        }catch(Exception excp){
            log.error("LLM API call failed", excp);
            llmresponse.setResponse("");
        }

        return llmresponse.getResponse();
    }

    private String createRequestBody(String exception, String codeSnippet) {

        StringBuilder sb = new StringBuilder();
        sb.append("// Code snippet with an exception");
        sb.append(System.lineSeparator());
        sb.append(codeSnippet);
        sb.append(System.lineSeparator());
        sb.append("// Exception \n");
        sb.append(exception);
        return sb.toString();
    }

    private String extractCodeSnippetfromMethod(String downloadPath, ErrorRequest error) throws IOException {
        String fileName = error.getClassName().concat(".java");
        log.info("Fetch fileName :"+ fileName);
        String methodName = error.getMethodName();
        log.info(" Method Name {}", methodName);
        File file = findFile(downloadPath,fileName);
        log.info("Start reading file");
        //String fileContents = readFile(file.toPath());
        List<String> fileContentList = Files.readAllLines(file.toPath());
       // log.info("file Contents {}", fileContents);
        MethodDetails details = extractMethodContent(file.getAbsolutePath().toString(),methodName);
        log.info("methodSnippet {}",details);
        String errorFixCode = processLLM(error.getException(),details.getContent());
        log.info("errorFixCode {}", errorFixCode);
        List<String> fixedCodeList = List.of(errorFixCode.split("\\\\n"));
        log.info("fixedCodeList {}",fixedCodeList);
        List<String> fixedCodeList1 = new ArrayList<>();
        fixedCodeList1.add(errorFixCode);
       /* if(fileContents.contains(details.getContent())){
            log.info("fileContents.contains(methodSnippet)");
        }
        else {
            log.info("fileContents.doesnot contains(methodSnippet)");
        }*/
        //for(int i=0;i<deta)
        List<String> modifiedList = replaceContent(fileContentList,fixedCodeList,details.getStartLine(),
                details.getEndLine());
       /* String updatedFileContent = fileContents.replace(details.getContent(),errorFixCode);
        log.info("updatedFileContent {}", updatedFileContent);*/
        //replaceFileContent(file.toPath().toString(),modifiedList.toString());
        replaceFileContent(file.toPath().toString(),modifiedList);
        return "";
    }

    private void replaceFileContent(String filePath, List<String> modifiedList) throws IOException {
        FileWriter writer = new FileWriter(filePath);

        // Loop through the list of strings and write each one to the file
        for (String line : modifiedList) {
            writer.write(line);
            writer.write(System.lineSeparator());
        }
        // Close the file writer
        writer.close();
    }

    public List<String> replaceContent(List<String> originalList, List<String> replacementList, int startLine, int endLine) {
        if (startLine < 0 || endLine >= originalList.size() || startLine > endLine) {
            log.info("Invalid startLine or endLine parameters.");
            log.info("originalList {}",originalList);
            return originalList;
        }

        List<String> modifiedList = new ArrayList<>(originalList.subList(0, startLine-1));
        log.info("modifiedList 1 : {}",modifiedList);
        modifiedList.addAll(replacementList);
        log.info("modifiedList 2 : {}",modifiedList);
        modifiedList.addAll(originalList.subList(endLine + 1, originalList.size()));
        log.info("modifiedList {}",modifiedList);
        return modifiedList;
    }

    //read file and convert to single string
    public String readFile(Path filePath) throws IOException {
        log.info("Convert file to String");

        //Path path = Path.of(filePath);
        byte[] bytes = Files.readAllBytes(filePath);
        log.info("Convert file to String Ends");
        return new String(bytes, StandardCharsets.UTF_8);
    }


    public void replaceFileContent(String filePath, String newContent) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            writer.write(newContent);
        }
    }


    private void cloneGitRepo(String repositoryURL,String downloadPath, String branchName, ErrorRequest error) throws GitAPIException, IOException {
        log.info("cloneGitRepo:: repositoryURL {}, downloadPath {}",repositoryURL,downloadPath);
        try {
            log.info("Check if download Path contains git repo");
            deleteFolder(Path.of(downloadPath));
        } catch (IOException e) {
            log.error("git repo doesn't exists");
        }
        log.info("Cloning Starts");
       Git git =  Git.cloneRepository()
                .setURI(repositoryURL)
                .setCredentialsProvider(new UsernamePasswordCredentialsProvider(username, "GitHub@OL2$"))
               .setBranch("brokenbranch")
                .setDirectory(new File(downloadPath)).call();
       //Use Project token provided by Chandra

        log.info("Cloning Ends");
        // Checkout the base branch
       /* git.checkout()
                .setName("brokenbranch")
                .call();*/

        //String username = "vikashkumar805";
        String key = "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";

        log.info("create Branch in Git with name {}", branchName.concat(("_codefix")));
        git.branchCreate().setName(branchName.concat("_codefix")).call();
        git.checkout().setName(branchName.concat("_codefix")).call();

        log.info("Call extractCodeSnippetfromMethod");
        extractCodeSnippetfromMethod (downloadPath,error);
        String commitMsg = "Initial commit";

        log.info("Git Add initiated");
        git.add().addFilepattern(".").call();
        log.info("Git Commit initiated");
         git.commit().setMessage(commitMsg).call();
        log.info("Git Push initiated");
       git.push().setCredentialsProvider
                (new UsernamePasswordCredentialsProvider
                        (username, "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"))
                .call();
        log.info("Git Push completed");
        git.getRepository().close();
        /*git.push()
                .setCredentialsProvider(new UsernamePasswordCredentialsProvider("vikashkumar805", "GitHub9467@OL2$"))
                .setRemote("origin").setRefSpecs(new RefSpec("refs/heads/" + branchName.concat("_codefix") + ":refs/heads/" + branchName.concat("_codefix")))
                //.setRefSpecs("refs/heads/" + branchName + ":refs/heads/" + branchName)
                .call();*/

        // Close the Git repository
        //git.close();
        log.info("Git closed successfully");
        processPullRequest(error);
        createBinaries(downloadPath);


    }

    private void createBinaries(String downloadPath) {
        String projectPath = String.valueOf(Path.of(downloadPath).resolve("projectapp"));
        log.info("projectPath {}",projectPath);
       // String downloadPathUpdated = downloadPath.concat("/productapp");
            GradleConnector connector = GradleConnector.newConnector();
            log.info("Gradle Connector {}",connector);
            connector.forProjectDirectory(new File(projectPath));

            ProjectConnection connection = connector.connect();
            try {
                connection.newBuild().forTasks("clean", "build").run();
                log.info("Project built successfully.");
            } catch (Exception e) {
                log.error("Failed to build project: " + e.getMessage());
            } finally {
                connection.close();
            }
    }

    private String processPullRequest(ErrorRequest error) {

        log.info("processPullRequest STARTS");
        String responseBody = "";
        try {
            RestTemplate restTemplate = new RestTemplate();

            GitPRRequest request = new GitPRRequest();
            request.setTitle("Fix");
            request.setBase("brokenbranch");
            request.setHead(error.getIssueId().concat("_codefix"));

            String key = "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";
            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Accept", "application/vnd.github+json");
            headers.set("Authorization", "Bearer " + "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");
            headers.set("X-GitHub-Api-Version", "2022-11-28");

            // Set request entity
            HttpEntity<GitPRRequest> requestEntity = new HttpEntity<>(request, headers);

            // Make POST request
            log.info("Calling PR API STARTS");
            ResponseEntity<String> responseEntity =
                    restTemplate.exchange("https://api.github.com/repos/cyalla/productrepo/pulls", HttpMethod.POST, requestEntity, String.class);
            log.info("Calling Git PR API ENDS");
            // Get response body
            responseBody = responseEntity.getBody();

            log.info("responseBody {}", responseBody.toString());
            log.info("responseEntity.getStatusCode() {}", responseEntity.getStatusCode());
        }catch(Exception excp){
            log.error("Git PR API call failed", excp);
        }
        log.info("processPullRequest ENDS");
        return responseBody;
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

    public MethodDetails extractMethodContent(String fileName, String methodName) {
        MethodDetails details = new MethodDetails();
        StringBuilder result = new StringBuilder();
        log.info("extractMethodContent STARTS");
        List<String> methodContent = new ArrayList<>();
        boolean inMethod = false;
        int openBracesCount = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            String line;
            int lineCounter = 0;
            while ((line = reader.readLine()) != null) {
                lineCounter++;
                if (line.contains(methodName) && line.contains("{")) {
                    details.setStartLine(lineCounter);
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
                        details.setEndLine(lineCounter);
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
        details.setContent(result.toString());
        return details;
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
