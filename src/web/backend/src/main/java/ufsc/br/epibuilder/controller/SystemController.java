package ufsc.br.epibuilder.controller;

import ufsc.br.epibuilder.dto.SystemStatus;
import ufsc.br.epibuilder.service.SystemStatusService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for application health and status checks.
 * This endpoint is polled by the frontend during application startup.
 */
@RestController
@RequestMapping("/api/system")
public class SystemController {

    private final SystemStatusService statusService;

    public SystemController(SystemStatusService statusService) {
        this.statusService = statusService;
    }

    /**
     * Retrieves the current application status (READY, INITIALIZING_DB, etc.).
     * 
     * @return ResponseEntity containing the SystemStatus DTO.
     */
    @GetMapping("/status")
    public ResponseEntity<SystemStatus> getStatus() {
        return ResponseEntity.ok(statusService.getStatus());
    }
}