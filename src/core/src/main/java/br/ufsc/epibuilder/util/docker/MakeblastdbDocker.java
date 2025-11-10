package br.ufsc.epibuilder.util.docker;

import java.nio.file.Path;
import java.util.List;


public class MakeblastdbDocker extends AbstractDockerExecutor {
    public MakeblastdbDocker(Path db) {
        super("staphb/blast:2.17.0",
                List.of("--rm"),
                List.of("makeblastdb",
                        "-in", db.toString(),
                        "-dbtype", "prot" ,
                        "-out", db.toString()),
                false);
    }
}