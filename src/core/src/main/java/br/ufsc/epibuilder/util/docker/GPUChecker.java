package br.ufsc.epibuilder.util.docker;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

public class GPUChecker extends AbstractDockerExecutor {
    private Integer exitCode;
    public GPUChecker(String gpuOptions) {
        super("bioinfoufsc/bepipred3",
                List.of("--rm",
                        "--gpus all", gpuOptions),
                List.of("python3","-u","bepipred3_custom.py","-v"),
                false);
    }
    public boolean hasGpu() {
        int exitCode = call();
        if (exitCode == 0) {
            return (getOutput().contains("GPU device detected: cuda"));
        }
        return false;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Tem GPU? "+new GPUChecker("").hasGpu());
        System.out.println("Tem GPU? "+new GPUChecker("--runtime=nvidia").hasGpu());
    }
}
