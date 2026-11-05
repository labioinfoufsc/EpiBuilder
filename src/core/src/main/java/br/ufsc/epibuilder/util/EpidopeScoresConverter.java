package br.ufsc.epibuilder.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Converts Epidope's TSV (position/header, aminoacid, score) to a CSV with
 * Accession, Residue and two identical score columns.
 *
 * Usage: java EpidopeScoresConverter <input.tsv-or-csv> <output.csv>
 *
 */
public class EpidopeScoresConverter {

    public static void main(String[] args) {

        args = new String[]{"/bioinformatics/EpiBuilder/src/core/pipeline/fasta/epidope_results/epidope_scores.csv",
                "/bioinformatics/EpiBuilder/src/core/pipeline/fasta/epidope_results/epidope_scores_formated.csv"};

        Path input = Path.of(args[0]).toAbsolutePath().normalize();
        Path output = Path.of(args[1]).toAbsolutePath().normalize();
        if (input.equals(output)) {
            System.err.println("Input and output must be different files.");
            System.exit(2);
        }

        try {
            convert(input, output);
        } catch (IOException | IllegalArgumentException e) {
            e.printStackTrace();
            System.err.println("Conversion error: " + e.getMessage());
            System.exit(1);
        }
    }

    public static void convert(Path input, Path output)
            throws IOException {
        String header = "Accession,Residue,Epidope score,Epidope linear epitope score";

        int proteins = 0;
        long residues = 0;
        String accession = null;

        try (BufferedReader reader = Files.newBufferedReader(input, StandardCharsets.UTF_8);
             BufferedWriter writer = Files.newBufferedWriter(output, StandardCharsets.UTF_8)) {

            writer.write(header);
            writer.newLine();

            String line;
            long lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (lineNumber == 1 && line.startsWith("\uFEFF")) {
                    line = line.substring(1); // UTF-8 BOM
                }
                if (line.isBlank() || line.equals("position/header\taminoacid\tscore")) {
                    continue;
                }
                if (line.startsWith(">")) {
                    accession = line.substring(1);
                    proteins++;
                    continue;
                }

                if (accession == null) {
                    throw new IllegalArgumentException("Missing FASTA header before data at line " + lineNumber);
                }

                String[] fields = line.split("\t", -1);
                if (fields.length != 3) {
                    throw new IllegalArgumentException("Expected 3 tab-separated fields at line " + lineNumber);
                }

                String position = fields[0].trim();
                String residue = fields[1].trim();
                String score = fields[2].trim();
                if (!position.matches("[1-9][0-9]*") || !residue.matches("[A-Za-z*]") ||
                        !score.matches("[+-]?(?:[0-9]+(?:\\.[0-9]*)?|\\.[0-9]+)(?:[eE][+-]?[0-9]+)?")) {
                    throw new IllegalArgumentException("Invalid position, residue or score at line " + lineNumber);
                }

                // Deliberately write the same Epidope score into both columns.
                writer.write(csv(accession) + "," + csv(residue) + "," + score + "," + score);
                writer.newLine();
                residues++;
            }
        }

        System.out.printf(Locale.ROOT, "Converted %d proteins and %d residues to %s%n",
                proteins, residues, output);
    }

    private static String csv(String text) {
        if (text.contains(",") || text.contains("\"") || text.contains("\n")) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }
}
