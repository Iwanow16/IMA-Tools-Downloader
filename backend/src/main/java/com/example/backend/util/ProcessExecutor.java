package com.example.backend.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Component
@Slf4j
public class ProcessExecutor {
    
    public static String executeCommand(List<String> command) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        Process process = processBuilder.start();
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
             BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
            
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line);
            }
            
            StringBuilder error = new StringBuilder();
            while ((line = errorReader.readLine()) != null) {
                error.append(line);
            }
            
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException("Command failed with exit code " + exitCode + ": " + error.toString());
            }
            
            return output.toString();
        }
    }
    
    public static ProcessResult executeCommandWithProgress(List<String> command, Consumer<Integer> progressCallback) 
            throws IOException, InterruptedException {
        
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        Process process = processBuilder.start();
        
        Thread outputThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.debug("Process output: {}", line);
                    
                    // Parse progress from output
                    Integer progress = parseProgress(line);
                    if (progress != null && progressCallback != null) {
                        progressCallback.accept(progress);
                    }
                }
            } catch (IOException e) {
                log.error("Error reading process output", e);
            }
        });
        
        Thread errorThread = new Thread(() -> {
            try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = errorReader.readLine()) != null) {
                    log.error("Process error: {}", line);
                }
            } catch (IOException e) {
                log.error("Error reading process error", e);
            }
        });
        
        outputThread.start();
        errorThread.start();
        
        boolean finished = process.waitFor(30, TimeUnit.MINUTES);
        if (!finished) {
            process.destroy();
            throw new RuntimeException("Command timed out after 30 minutes");
        }
        
        outputThread.join(5000);
        errorThread.join(5000);
        
        return new ProcessResult(process.exitValue());
    }
    
    private static Integer parseProgress(String line) {
        // Parse progress from yt-dlp output
        // Example: [download]  12.5% of 123.45MiB at 456.78KiB/s ETA 00:12
        if (line.contains("[download]") && line.contains("%")) {
            try {
                int percentIndex = line.indexOf("%");
                int startIndex = Math.max(0, percentIndex - 10);
                String progressStr = line.substring(startIndex, percentIndex + 1);
                progressStr = progressStr.replaceAll("[^0-9.%]", "").replace("%", "").trim();
                return Integer.parseInt(progressStr);
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }
    
    public static class ProcessResult {
        private final int exitCode;
        private String output;
        private String error;
        
        public ProcessResult(int exitCode) {
            this.exitCode = exitCode;
        }
        
        public int getExitCode() {
            return exitCode;
        }
        
        public String getOutput() {
            return output;
        }
        
        public String getError() {
            return error;
        }
        
        public boolean isSuccess() {
            return exitCode == 0;
        }
    }
}