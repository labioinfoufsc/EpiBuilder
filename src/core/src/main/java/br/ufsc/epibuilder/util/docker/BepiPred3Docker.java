package br.ufsc.epibuilder.util.docker;

import java.nio.file.Path;
import java.util.List;


public class BepiPred3Docker extends AbstractDockerExecutor {
    /**
     *
     * @param tmpFile
     * @param gpuAvailable
     */

    public BepiPred3Docker(Path tmpFile, boolean gpuAvailable) {
        super("bioinfoufsc/bepipred3",
                List.of("--rm",
                        gpuAvailable ? " --gpus all" : ""),
                List.of("python3","-u","bepipred3_custom.py","-i", tmpFile.toString(), "-o", tmpFile.getParent().toString()),
                false);
    }
}
