package br.ufsc.epibuilder.util.docker;

import java.nio.file.Path;
import java.util.List;


public class BepiPred3Docker extends AbstractDockerExecutor {
    public BepiPred3Docker(Path tmpDir, Path tmpFile, boolean gpuAvailable) {
        super("bioinfoufsc/bepipred3",
                tmpDir,
                List.of("--rm",
                        gpuAvailable ? "--runtime=nvidia --gpus all" : ""),
                List.of("python3","-u","bepipred3_custom.py","-i", tmpFile.toString(), "-o", tmpDir.toString()),
                false);
    }
}
