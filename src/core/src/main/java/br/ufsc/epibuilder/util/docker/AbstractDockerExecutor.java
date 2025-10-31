package br.ufsc.epibuilder.util.docker;

import lombok.Getter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

import static br.ufsc.epibuilder.util.docker.AbstractDockerExecutor.LogLevel.*;

public abstract class AbstractDockerExecutor implements Callable<Integer> {
    public enum LogLevel {
        DEBUG, INFO, WARN, ERROR
    }

    @Getter
    private String container;
    private Path tmpDir;
    private List<String> options;
    private boolean ignoreExitCode = false;
    private List<String> commandArgs;

    @Getter
    private String commandExecuted = "";

    private static String EPIBUILDER_VOLUME = System.getenv().getOrDefault("EPIBUILDER_VOLUME", "/tmp/epibuilder");

    public AbstractDockerExecutor(String container, Path tmpDir, List<String> options, List<String> commandArgs, boolean ignoreExitCode) {
        this.container = container;
        this.tmpDir = tmpDir;
        this.options = options;
        this.ignoreExitCode = ignoreExitCode;
        this.commandArgs = commandArgs;
    }

    public void addOption(String option) {
        options.add(option);
    }


    public int executeDockerCommand() {
        try {
            String optionsCmd = String.join(" ", options);

            String cmd = String.join(" ", commandArgs);
            List<String> command = new ArrayList<>(
                    Arrays.asList("bash", "-c", String.format("docker run %s -v %s:/tmp/epibuilder " + "%s %s",
                            optionsCmd,
                            EPIBUILDER_VOLUME,
                            container,
                            cmd)));
            ProcessBuilder pb = new ProcessBuilder(command);
            this.commandExecuted = String.join(" ", command);
            System.out.println(this.commandExecuted);
            pb.redirectErrorStream(true);

            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                    log(line, LogLevel.INFO);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                log("Error while executing docker command: " + command, ERROR);
                log("Docker command exited with code " + exitCode, ERROR);
                return exitCode;
            }
            return 0;

        } catch (Exception e) {
            log("Error while executing docker command.", ERROR);
            e.printStackTrace();
            System.exit(1);
        }
        return 0;
    }

    public void log(String message, LogLevel logLevel) {
        try {
            System.out.printf("[%s] %s", logLevel, message);
            BufferedWriter logWriter = null;

            logWriter = Files.newBufferedWriter(tmpDir.resolve("pipeline.log"), StandardOpenOption.CREATE, StandardOpenOption.APPEND);

            logWriter.append(String.format("[%s] %s", logLevel, message));
            logWriter.newLine();
            logWriter.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Integer call() {
        try {
            int exitCode = executeDockerCommand();
            if (exitCode != 0) {
                new Exception("Error executing "+container);
            }else{
                log("Success executing docker command: " + getCommandExecuted(), LogLevel.INFO);
            }
        } catch (Exception e) {
            e.printStackTrace();
            log("Error executing docker command: " + getCommandExecuted(), LogLevel.ERROR);
            return 1;
        }
        return 0;
    }
}
