/**
 * Interface representing the current status of the asynchronous UniProt
 * database download task.
 *
 * This structure mirrors the Java UniProtDownloadStatus DTO/Record in the backend.
 */
export interface UniProtDownloadStatus {
    /** If the download task is currently running. */
    inProgress: boolean;

    /** A detailed, human-readable message about the current progress (e.g., MB processed). */
    progressMessage: string;

    /** Indicates the final outcome (true if completed successfully, false if failed). */
    success: boolean;
}