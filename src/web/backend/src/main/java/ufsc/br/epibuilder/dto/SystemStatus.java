package ufsc.br.epibuilder.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * Data Transfer Object for conveying the application's current readiness state.
 */
@Getter
@Setter
public class SystemStatus {
    private final String status;
    private final String message;

    public SystemStatus(String status, String message) {
        this.status = status;
        this.message = message;
    }
}