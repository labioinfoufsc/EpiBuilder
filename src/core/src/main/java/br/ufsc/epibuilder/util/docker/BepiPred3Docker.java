package br.ufsc.epibuilder.util.docker;

import java.nio.file.Path;
import java.util.List;



package br.ufsc.epibuilder.util.docker;

import java.nio.file.Path;
import java.util.List;

public class BepiPred3Docker extends AbstractDockerExecutor {

    public BepiPred3Docker(Path tmpFile, boolean gpuAvailable) {
        super(
            "bioinfoufsc/bepipred3",

            gpuAvailable
                ? List.of("--rm", "--gpus", "all")
                : List.of("--rm"),

            List.of(
                "python3",
                "-u",
                "bepipred3_custom.py",
                "-i",
                tmpFile.toString(),
                "-o",
                tmpFile.getParent().toString()
            ),

            false
        );
    }
}
