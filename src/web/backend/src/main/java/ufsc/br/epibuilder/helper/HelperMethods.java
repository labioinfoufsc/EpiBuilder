package ufsc.br.epibuilder.helper;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class HelperMethods {

    public static int countSequences(Path filePath) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(filePath)) {
            int count = 0;
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith(">")) {
                    count++;
                }
            }
            return count;
        }
    }
    
}
