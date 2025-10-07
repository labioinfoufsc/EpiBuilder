package ufsc.br.epibuilder.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ufsc.br.epibuilder.dto.UserDTO;
import ufsc.br.epibuilder.model.User;
import ufsc.br.epibuilder.service.AuthService;

import lombok.extern.slf4j.Slf4j;

/**
 * Controller responsible for authentication-related endpoints.
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    /**
     * Handles user login requests.
     *
     * @param request the user credentials for authentication
     * @return a ResponseEntity containing the authenticated user data or an error message
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody User request) {
        log.info("Attempting login for user: {}", request.getUsername());

        try {
            UserDTO user = authService.authenticate(request);
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            log.warn("Authentication failed for user: {}", request.getUsername());
            log.warn(e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
