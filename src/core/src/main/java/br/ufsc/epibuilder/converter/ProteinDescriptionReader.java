package br.ufsc.epibuilder.converter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class ProteinDescriptionReader {
    /**
            * Reads a TSV file using Java 8 Streams, skips the header, and maps the first
            * two columns (ID and Description).
            *
            * @param filePath The full path to the TSV file.
            * @return A HashMap where the key is the ID (String) and the value is the Description (String).
            * @throws IOException If an error occurs during file reading.
            */
    public static HashMap<String, String> readTsvToMap(File filePath) throws IOException {
        if(filePath == null) {
            return new HashMap<>();
        }

        final String DELIMITER = "\t";
        Path path = filePath.toPath();

        // 1. Usar try-with-resources para garantir que o Stream de linhas seja fechado
        try (Stream<String> lines = Files.lines(path)) {

            return lines
                    // 2. Pular o Cabeçalho: Descartar a primeira linha
                    .skip(1)

                    // 3. Filtrar Linhas Vazias
                    .filter(line -> !line.trim().isEmpty())

                    // 4. Mapear: Transformar cada linha em um par Chave-Valor
                    .map(line -> line.split(DELIMITER))

                    // 5. Filtrar: Garantir que a linha tenha pelo menos 2 colunas
                    .filter(columns -> columns.length >= 2)

                    // 6. Coletar: Criar o HashMap
                    // O collect(Collectors.toMap(...)) é a forma mais limpa de construir um Map a partir de um Stream.
                    .collect(HashMap::new,
                            (map, columns) -> map.put(columns[0].trim(), columns[1].trim()),
                            Map::putAll); // Merge function para processamento paralelo (ótimo para arquivos grandes!)

        } catch (Exception e) {
            // Em vez de relançar, você pode capturar e reempacotar se quiser
            throw new IOException("Error processing TSV file: " + filePath, e);
        }
    }

    // --- Exemplo de Uso ---

    public static void main(String[] args) {
        // Crie um arquivo de exemplo chamado "proteins.tsv"
        String filePath = "/bioinformatic/labioinfoufsc/EpiBuilder/www/admin/Influenza_A_20251007_020645/proteins.tsv";

        try {

            Map<String, String> proteinDescriptions = readTsvToMap(new File(filePath));

            System.out.println("\n--- Data successfully loaded (Modern Method) ---");
            System.out.println("Total records: " + proteinDescriptions.size());
            System.out.println("P12345 Description: " + proteinDescriptions.get("P12345"));

        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
        }
    }
}
