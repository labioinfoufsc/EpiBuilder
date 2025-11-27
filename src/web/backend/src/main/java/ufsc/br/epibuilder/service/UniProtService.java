package ufsc.br.epibuilder.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import ufsc.br.epibuilder.dto.UniProtDownloadStatus;
import ufsc.br.epibuilder.helper.HelperMethods;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.GZIPInputStream;
import ufsc.br.epibuilder.model.Database;
import ufsc.br.epibuilder.service.DatabaseService;
import java.nio.file.Path;

/**
 * Service to handle asynchronous UniProt database download and decompression.
 * It is called by the API and runs in a separate thread.
 */
@Service
@Slf4j
public class UniProtService {

    private static final String UNIPROT_URL = "https://ftp.uniprot.org/pub/databases/uniprot/current_release/knowledgebase/complete/uniprot_sprot.fasta.gz";
    private static final String DB_DIRECTORY = "/tmp/epibuilder/db";
    private static final int BUFFER_SIZE = 8192; // 8KB buffer for streams
    private static final long LOG_THRESHOLD_BYTES = 10 * 1024 * 1024; // Log progress every 10MB
    private final DatabaseService databaseService;

    public UniProtService(DatabaseService databaseService) {
        this.databaseService = databaseService;
    }

    // Atomic state to track the progress of the single download task
    private final AtomicReference<UniProtDownloadStatus> currentStatus = new AtomicReference<>(
            new UniProtDownloadStatus(false, "Idle", false));

    /**
     * Triggers the UniProt download process asynchronously.
     */
    @Async
public void startDownload() {
    if (currentStatus.get().isInProgress()) {
        log.info("[UniProtService] Download already in progress. Skipping request.");
        return;
    }

    updateStatus(true, "Initiating download...", false);
    log.info("[UniProtService] UniProt download initiated asynchronously.");

    String today = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
    String fileName = "uniprot_sprot_" + today + ".fasta";
    Path targetPath = Paths.get(DB_DIRECTORY, fileName);

    Path directory = Paths.get(DB_DIRECTORY);
    if (!Files.exists(directory)) {
        try {
            Files.createDirectories(directory);
        } catch (IOException e) {
            updateStatus(false, "Failed to create directory: " + e.getMessage(), false);
            return;
        }
    }

    try {
        downloadAndDecompress(UNIPROT_URL, targetPath);

        int sequenceCount = HelperMethods.countSequences(targetPath);

        Database db = new Database();
        db.setAlias("uniprot_sprot_" + today);
        db.setFileName(fileName);
        db.setAbsolutePath(targetPath.toString());
        db.setDate(LocalDateTime.now());
        db.setAmountSequences(sequenceCount);

        Database created = databaseService.save(db);
        log.info("[UniProtService] Database persisted with ID: " + created.getId());

        ProcessBuilder pb = new ProcessBuilder(
                "makeblastdb",
                "-in", targetPath.toString(),
                "-dbtype", "prot",
                "-out", targetPath.toString()
        );
        Process process = pb.start();
        log.info("[UniProtService] BLAST database process started with PID: " + process.pid());

        updateStatus(false, "Download, decompression and persistence successful.", true);
    } catch (IOException e) {
        System.err.println("[UniProtService] Download failed: " + e.getMessage());
        updateStatus(false, "Download failed: " + e.getMessage(), false);
    } catch (Exception e) {
        System.err.println("[UniProtService] Error persisting database: " + e.getMessage());
        updateStatus(false, "Persistence failed: " + e.getMessage(), false);
    }
}


    /**
     * Downloads a GZIP file and decompresses it on-the-fly to the target path.
     * Uses a temporary file (.tmp) to ensure atomicity.
     */
    private void downloadAndDecompress(String urlString, Path targetPath) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(30_000);
        conn.setReadTimeout(60_000);

        int responseCode = conn.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new IOException("Failed to download UniProt: HTTP " + responseCode);
        }

        Path tempPath = targetPath.resolveSibling(targetPath.getFileName() + ".tmp");

        try (InputStream in = conn.getInputStream();
                GZIPInputStream gzipIn = new GZIPInputStream(in);
                FileOutputStream out = new FileOutputStream(tempPath.toFile())) {

            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            long totalBytes = 0;
            long lastLogBytes = 0;
            long startTime = System.currentTimeMillis();

            log.info("[UniProtService] Download started...");

            while ((bytesRead = gzipIn.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
                totalBytes += bytesRead;

                if (totalBytes - lastLogBytes > LOG_THRESHOLD_BYTES) {
                    double mbProcessed = totalBytes / (1024.0 * 1024.0);
                    String progressMsg = String.format("Downloading and Decompressing: %.2f MB processed...",
                            mbProcessed);
                    updateStatus(true, progressMsg, false);
                    log.info("[UniProtService] " + progressMsg);
                    lastLogBytes = totalBytes;
                }
            }

            long duration = (System.currentTimeMillis() - startTime) / 1000;
            log.info("[UniProtService] Download completed in %d seconds. Finalizing file...%n", duration);
        }

        Files.move(tempPath, targetPath, StandardCopyOption.REPLACE_EXISTING);
        log.info("[UniProtService] UniProt Swiss-Prot saved successfully at: " + targetPath);
    }

    /**
     * Retrieves the current status of the asynchronous download.
     */
    public UniProtDownloadStatus getStatus() {
        return currentStatus.get();
    }

    /**
     * Helper to update the atomic status state.
     */
    private void updateStatus(boolean inProgress, String message, boolean success) {
        currentStatus.set(new UniProtDownloadStatus(inProgress, message, success));
    }
}
