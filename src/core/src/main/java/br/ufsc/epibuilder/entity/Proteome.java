package br.ufsc.epibuilder.entity;

import br.ufsc.epibuilder.Parameters;
import lombok.Data;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 *
 * @author renato
 */

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Data
public class Proteome {
    private String organism;
    private File originalFile;
    private File file;

    public Proteome(String organism, File originalFile) throws IOException {
        this.organism = organism;
        this.originalFile = originalFile;

        File destDir = new File(Parameters.DESTINATION_FOLDER);
        if (!destDir.exists()) {
            destDir.mkdirs();
        }

        this.file = new File(destDir, originalFile.getName());

        Files.copy(originalFile.toPath(), this.file.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }
}
