package br.ufsc.epibuilder.util.docker;

import java.nio.file.Path;
import java.util.List;

public class EpidopeDocker extends AbstractDockerExecutor {

    public EpidopeDocker(Path tmpFile) {
        super(
            "flomock/epidope:v0.3", List.of("--rm"),

            List.of(
                "-i",
                tmpFile.toString(),
                "-o",
                tmpFile.getParent().toString()
            ),
            false
        );
    }
}
