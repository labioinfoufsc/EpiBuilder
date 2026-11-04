package br.ufsc.epibuilder.util.docker;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

public class GPUChecker extends AbstractDockerExecutor {
    private Integer exitCode;
    public GPUChecker() {
        super("bioinfoufsc/bepipred3",
                List.of("--rm",
                        "--gpus", "all"),
                List.of("python3","-u","bepipred3_custom.py","-h"),
                false);
    }
    public boolean hasGpu() {
        int exitCode = call();
        System.out.println("GPU check exit code:"+ exitCode);
        if (exitCode == 0) {
            System.out.println(getOutput());
            return (getOutput().contains("GPU device detected: cuda"));
        }
        return false;
    }
    
}
