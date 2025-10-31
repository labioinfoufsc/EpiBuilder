package br.ufsc.epibuilder.util.docker;

import java.nio.file.Path;
import java.util.List;

public class GPUChecker extends AbstractDockerExecutor {
    private Integer exitCode;
    public GPUChecker(Path tmpDir) {
        super("ubuntu",
                tmpDir,
                List.of("--rm", "--runtime=nvidia", "--gpus all"),
                List.of("nvidia-smi > /dev/null 2>&1"),
                false);
    }
}
