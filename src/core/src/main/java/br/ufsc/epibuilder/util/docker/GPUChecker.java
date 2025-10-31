package br.ufsc.epibuilder.util.docker;

import java.nio.file.Path;
import java.util.List;

public class GPUChecker extends AbstractDockerExecutor {
    private Integer exitCode;
    public GPUChecker(Path tmpDir, String gpuOptions) {
        super("ubuntu",
                tmpDir,
                List.of("--rm",gpuOptions,"--gpus all"),
                List.of("nvidia-smi > /dev/null 2>&1"),
                false);
    }
}
