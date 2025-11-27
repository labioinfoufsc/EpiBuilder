package ufsc.br.epibuilder.service;

import ufsc.br.epibuilder.dto.SystemStatus;
import org.springframework.stereotype.Service;

/**
 * Singleton service to hold and manage the application's current operational
 * status.
 * This status is critical during the initial data loading phase.
 */
@Service
public class SystemStatusService {

    // Status is volatile to ensure visibility across different threads (DataLoader
    // vs Controller)
    private volatile String currentStatus = "STARTING";
    private volatile String currentMessage = "Application context is initializing...";

    /**
     * Updates the current status and message.
     * 
     * @param status  The new status string (e.g., READY, INITIALIZING_DB).
     * @param message The detailed message for the user.
     */
    public void setStatus(String status, String message) {
        this.currentStatus = status;
        this.currentMessage = message;
    }

    /**
     * Retrieves the current system status DTO.
     * 
     * @return The current SystemStatus object.
     */
    public SystemStatus getStatus() {
        return new SystemStatus(this.currentStatus, this.currentMessage);
    }
}