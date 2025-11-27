package ufsc.br.epibuilder.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Data Transfer Object representing the current status of the asynchronous
 * UniProt
 * database download task.
 *
 * @param inProgress      If the download task is currently running.
 * @param progressMessage A detailed, human-readable message about the current
 *                        progress (e.g., MB processed).
 * @param success         Indicates the final outcome (true if completed
 *                        successfully, false if failed).
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UniProtDownloadStatus {
    private boolean inProgress;
    private String progressMessage;
    private boolean success;
}