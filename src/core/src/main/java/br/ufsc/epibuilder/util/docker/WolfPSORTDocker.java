package br.ufsc.epibuilder.util.docker;

import java.nio.file.Path;
import java.util.List;


public class WolfPSORTDocker extends AbstractDockerExecutor {
    public WolfPSORTDocker(Path tmpDir, Path tmpFile, String localization, Path output) {
        super("bioinfoufsc/wolfpsort",
                tmpDir,
                List.of("--rm"),
                List.of("-i", tmpFile.toString(), "-s", localization, "-o", output.toString()),
                false);

    }
}
