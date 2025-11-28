package ufsc.br.epibuilder.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.net.http.HttpClient;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import ufsc.br.epibuilder.dto.IedbDownloadStatus;
import ufsc.br.epibuilder.model.Database;

@Service
@Slf4j
public class IedbService {

    private static final String IEDB_API = "https://query-api.iedb.org/epitope_export";
    private static final String DB_DIRECTORY = "/tmp/epibuilder/db";
    private static final int LIMIT = 10000;
    private static final int MAX_PAGES = 200;

    private final DatabaseService databaseService;
    private final IedbDownloadStatus status = new IedbDownloadStatus();

    public IedbService(DatabaseService databaseService) {
        this.databaseService = databaseService;
    }

    public IedbDownloadStatus getStatus() {
        return status;
    }

    @Async
    public void startDownload() {
        status.setInProgress(true);
        status.setProgressMessage("Starting IEDB download...");
        status.setSuccess(false);

        String today = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
        String fileName = "iedb_linear_" + today + ".fasta";
        Path targetPath = Paths.get(DB_DIRECTORY, fileName);

        try {
            Files.createDirectories(Paths.get(DB_DIRECTORY));
            try (BufferedWriter writer = Files.newBufferedWriter(targetPath)) {
                int totalSequences = 0;

                for (int page = 0; page < MAX_PAGES; page++) {
                    int offset = page * LIMIT;
                    URI uri = new URI(IEDB_API + "?limit=" + LIMIT + "&offset=" + offset + "&order=structure_id");

                    HttpRequest request = HttpRequest.newBuilder(uri)
                            .header("accept", "text/csv")
                            .GET()
                            .build();

                    HttpResponse<String> response = HttpClient.newHttpClient()
                            .send(request, HttpResponse.BodyHandlers.ofString());

                    if (response.body().trim().isEmpty())
                        break;

                    try (BufferedReader reader = new BufferedReader(new StringReader(response.body()))) {
                        reader.readLine(); // skip header
                        String line;
                        while ((line = reader.readLine()) != null) {
                            String[] cols = line.split(",", -1);
                            if (cols.length < 4)
                                continue;

                            String structureId = cols[0].trim();
                            String sequence = cols[1].trim().toUpperCase();
                            String antigen = cols[2].isEmpty() ? "Unknown" : cols[2];
                            String organism = cols[3].isEmpty() ? "Unknown" : cols[3];

                            if (sequence.isEmpty())
                                continue;

                            String fastaHeader = String.format(
                                    ">infectious-%s | %s | %s | http://www.iedb.org/epitope/%s",
                                    structureId, antigen, organism, structureId);
                            writer.write(fastaHeader);
                            writer.newLine();
                            writer.write(sequence);
                            writer.newLine();

                            totalSequences++;
                        }
                    }

                    status.setProgressMessage("Downloaded " + totalSequences + " sequences so far...");

                    if (response.body().split("\n").length < LIMIT)
                        break;
                }

                Database db = new Database();
                db.setAlias("iedb_linear_" + today);
                db.setFileName(fileName);
                db.setAbsolutePath(targetPath.toString());
                db.setDate(LocalDateTime.now());
                db.setAmountSequences(totalSequences);

                databaseService.save(db);

                status.setProgressMessage("IEDB download finished. " + totalSequences + " sequences saved.");
                status.setSuccess(true);

            }
        } catch (Exception e) {
            status.setProgressMessage("IEDB download failed: " + e.getMessage());
            status.setSuccess(false);
        } finally {
            status.setInProgress(false);
        }
    }

}
