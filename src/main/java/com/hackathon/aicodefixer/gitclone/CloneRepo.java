package com.hackathon.aicodefixer.gitclone;

import java.io.IOException;

public class CloneRepo {

    public static void cloneRepository(String repositoryUrl, String destinationPath) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command("git", "clone", repositoryUrl, destinationPath);

        Process process = processBuilder.start();
        int exitCode = process.waitFor();

        if (exitCode != 0) {
            throw new RuntimeException("Failed to clone repository. Exit code: " + exitCode);
        }
    }
}
