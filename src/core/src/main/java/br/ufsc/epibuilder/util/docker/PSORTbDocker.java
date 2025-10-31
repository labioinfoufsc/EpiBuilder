package br.ufsc.epibuilder.util.docker;

import java.nio.file.Path;
import java.util.List;


public class PSORTbDocker extends AbstractDockerExecutor {
    public PSORTbDocker(Path tmpDir, Path tmpFile, String localization, Path output) {
        super("bioinfoufsc/psortb",
                tmpDir,
                List.of("--rm"),
                List.of("-i", tmpFile.toString(), localization,"-o terse","-r", output.toString()),
                true);

    }
}
