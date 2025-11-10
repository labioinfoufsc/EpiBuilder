package br.ufsc.epibuilder.util.docker;

import java.nio.file.Path;
import java.util.List;


public class BlastpDocker extends AbstractDockerExecutor {
    public BlastpDocker(Path query, Path db, Path output) {
        super("staphb/blast:2.17.0",
                List.of("--rm"),
                List.of("blastp",
                        "-query", query.toString(),
                        "-db", db.toString() ,
                        "-outfmt", "\"6 qacc sacc pident qcovs qseq sseq\"" ,
                        "-task", "blastp-short" ,
                        "-word_size", "4",
                        ">", output.toString()),
                false);
    }
}
