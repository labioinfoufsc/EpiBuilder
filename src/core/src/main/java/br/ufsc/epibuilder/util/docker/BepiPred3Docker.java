package br.ufsc.epibuilder.util.docker;

import java.nio.file.Path;
import java.util.List;


public class BepiPred3Docker extends AbstractDockerExecutor {
    /**
     *
     * @param tmpDir
     * @param tmpFile
     * @param gpuAvailable
     * @param gpuOptions if your docker need more informations to enable gpu use, for instance: --runtime=nvidia
     */

    public BepiPred3Docker(Path tmpDir, Path tmpFile, boolean gpuAvailable, String gpuOptions) {
        super("bioinfoufsc/bepipred3",
                tmpDir,
                List.of("--rm",
                        gpuOptions,
                        gpuAvailable ? " --gpus all" : ""),
                List.of("python3","-u","bepipred3_custom.py","-i", tmpFile.toString(), "-o", tmpDir.toString()),
                false);
    }
}
