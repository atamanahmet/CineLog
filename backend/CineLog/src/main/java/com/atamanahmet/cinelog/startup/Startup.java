package com.atamanahmet.cinelog.startup;

import java.io.IOException;

import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.InputStreamReader;

@Component
public class Startup {
    private static final String PROJECT_FOLDER = "E:\\desktop25.04.2025\\page\\FrontEnd\\cinelog";

    @PostConstruct
    public void startupProcesses() {
        killAllNode();
        killRecEngine();
        startFrontend();
        startPythonRecommendationEngine();
        openVSCodeFolderByTitle(PROJECT_FOLDER);
    }

    private void killRecEngine() {
        try {
            new ProcessBuilder("powershell.exe", "/c",
                    "Get-CimInstance Win32_Process | " +
                            "Where-Object { $_.CommandLine -like '*rec.py*' } | " +
                            "ForEach-Object { Stop-Process -Id $_.ProcessId -Force }")
                    .start();
            System.out.println("Rec killed");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void killAllNode() {
        try {
            new ProcessBuilder("taskkill", "/F", "/IM", "node.exe").start();
            System.out.println("Node killed");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void startFrontend() {
        try {
            new ProcessBuilder(
                    "cmd.exe", "/c", "start", "cmd.exe", "/k",
                    "cd /d E:\\desktop25.04.2025\\page\\FrontEnd\\cinelog && npm run dev").start();
            System.out.println("Frontend started");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void startPythonRecommendationEngine() {
        try {
            new ProcessBuilder(
                    "cmd.exe", "/c", "start", "cmd.exe", "/k",
                    "cd /d E:\\desktop25.04.2025\\page\\RecommendationEngine\\recommendationService && python rec.py")
                    .start();
            System.out.println("RecEngine started");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean isVSCodeFolderOpen(String folderPath) {
        try {
            ProcessBuilder pb = new ProcessBuilder("powershell.exe", "/c",
                    "Get-CimInstance Win32_Process | " +
                            "Where-Object { $_.Name -eq 'Code.exe' -and $_.CommandLine -like '*" + folderPath
                            + "*' } | " +
                            "Select-Object -First 1");

            Process process = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;
            boolean hasOutput = false;
            while ((line = reader.readLine()) != null) {
                if (line.trim().length() > 0 && !line.contains("ProcessId")) {
                    hasOutput = true;
                    break;
                }
            }

            process.waitFor();
            reader.close();

            return hasOutput;

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void openVSCodeFolder(String folderPath) {
        if (isVSCodeFolderOpen(folderPath)) {
            System.out.println("VSCode already has this folder open: " + folderPath);
            return;
        }

        try {
            new ProcessBuilder("cmd.exe", "/c", "code", folderPath)
                    .start();
            System.out.println("Opening folder in VSCode: " + folderPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void openProjectInVSCode() {
        openVSCodeFolder(PROJECT_FOLDER);
    }

    public void openFolderInVSCode(String folderPath) {
        openVSCodeFolder(folderPath);
    }

    private boolean isVSCodeFolderOpenByTitle(String folderPath) {
        try {
            String folderName = new java.io.File(folderPath).getName();

            ProcessBuilder pb = new ProcessBuilder("powershell.exe", "/c",
                    "Get-Process | Where-Object { $_.ProcessName -eq 'Code' -and $_.MainWindowTitle -like '*"
                            + folderName + "*' } | " +
                            "Select-Object -First 1");

            Process process = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;
            boolean hasOutput = false;
            while ((line = reader.readLine()) != null) {
                if (line.trim().length() > 0 && !line.contains("ProcessName")) {
                    hasOutput = true;
                    break;
                }
            }

            process.waitFor();
            reader.close();

            return hasOutput;

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void openVSCodeFolderByTitle(String folderPath) {
        if (isVSCodeFolderOpenByTitle(folderPath)) {
            System.out.println("VSCode already has this folder open: " + folderPath);
            return;
        }

        try {
            new ProcessBuilder("cmd.exe", "/c", "code", folderPath)
                    .start();
            System.out.println("Opening folder in VSCode: " + folderPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
