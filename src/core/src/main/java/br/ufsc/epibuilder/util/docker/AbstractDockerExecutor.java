package br.ufsc.epibuilder.util.docker;

import lombok.Getter;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private FileWriter fileWriter;
    @Getter
    private String commandExecuted = "";
    private StringBuilder output = new StringBuilder();

    private static String EPIBUILDER_VOLUME = System.getenv().getOrDefault("EPIBUILDER_VOLUME", "/tmp/epibuilder");

    public AbstractDockerExecutor(String container, List<String> options, List<String> commandArgs, boolean ignoreExitCode) {
        this.container = container;
        this.options = options;
        this.ignoreExitCode = ignoreExitCode;
        this.commandArgs = commandArgs;

        Pattern pattern = Pattern.compile(".*/([^:]+)(?::.*)?");
        Matcher matcher = pattern.matcher(container);

        if (matcher.matches()) {
            String containerName = matcher.group(1);
            System.out.println(containerName);
            try {
                this.fileWriter = new FileWriter(containerName+".txt");// 👉 bepipred3
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

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
                    //System.out.println(line);
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
            return 1;
        }
    }

    // public void log(String message, LogLevel logLevel) {
    //         String msg = String.format("[%s] %s\n", logLevel, message);
    //         //System.out.println(msg);
    //         output.append(msg);
    //     try {
    //         fileWriter.write(msg);
    //         fileWriter.flush();
    //     } catch (IOException e) {
    //         throw new RuntimeException(e);
    //     }

    // }

public void log(String message, LogLevel logLevel) {
    String msg = String.format("[%s] %s", logLevel, message);

    // Print to console
    System.out.println(msg);
    System.out.flush();

    // Store in memory
    output.append(msg).append(System.lineSeparator());

    // Save to file
    if (fileWriter != null) {
        try {
            fileWriter.write(msg + System.lineSeparator());
            fileWriter.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

    // public Integer call() {
    //     try {
    //         int exitCode = executeDockerCommand();
    //         if (exitCode != 0) {
    //             new Exception("Error executing "+container);
    //         }else{
    //             log("Success executing docker command: " + getCommandExecuted(), LogLevel.INFO);
    //         }
    //     } catch (Exception e) {
    //         e.printStackTrace();
    //         log("Error executing docker command: " + getCommandExecuted(), LogLevel.ERROR);
    //         return 1;
    //     }
    //     return 0;
    // }

@Override
public Integer call() {
    try {
        int exitCode = executeDockerCommand();

        if (exitCode != 0) {
            log("Error executing docker command: " +
                getCommandExecuted(), LogLevel.ERROR);
            return exitCode;
        }

        log("Success executing docker command: " +
            getCommandExecuted(), LogLevel.INFO);

        return 0;

    } catch (Exception e) {
        e.printStackTrace();
        log("Error executing docker command: " +
            getCommandExecuted(), LogLevel.ERROR);
        return 1;

    } finally {
        if (fileWriter != null) {
            try {
                fileWriter.close();
            } catch (IOException e) {
                System.err.println(
                    "Error closing log file: " + e.getMessage()
                );
            }
        }
    }
}

    public String getOutput() {
        return output.toString();
    }
}
