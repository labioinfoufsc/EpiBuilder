package ufsc.br.epibuilder.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ufsc.br.epibuilder.dto.UserDTO;
import ufsc.br.epibuilder.model.User;
import ufsc.br.epibuilder.service.UserService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

/**
 * Controller responsible for handling user-related operations.
 */
@RestController
@Slf4j
@RequestMapping("/users")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);
    private final UserService userService;

    /**
     * Constructs a UserController with the specified UserService.
     *
     * @param userService the service handling user operations
     */
    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Retrieves a list of all users.
     *
     * @return a ResponseEntity containing the list of users or an internal server
     *         error status
     */
    @GetMapping
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        try {
            log.info("Attempting to list all users...");
            List<UserDTO> users = userService.getAllUsersExceptLogged();
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            log.error("Error retrieving users: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Updates an existing user.
     *
     * @param id   the ID of the user to update
     * @param user the updated user data
     * @return a ResponseEntity containing the updated user or a not found status
     */
    @PutMapping("/{id}")
    public ResponseEntity<UserDTO> updateUser(@PathVariable Long id, @Valid @RequestBody User user) {
        try {
            log.info("Attempting to update user with ID: {}", id);

            Optional<UserDTO> updatedUser = userService.updateUser(id, user);
            return updatedUser.map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Error updating user with ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Retrieves a user by their ID.
     *
     * @param id the ID of the user to retrieve
     * @return a ResponseEntity containing the user data if found, or a not found
     *         status
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long id) {
        try {
            log.info("Attempting to search user with ID: {}", id);
            Optional<UserDTO> user = userService.findUserById(id);
            if (user.isPresent()) {
                log.info("User found: {}", user.get());
                return ResponseEntity.ok(user.get());
            } else {
                log.warn("User with ID {} not found", id);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("Error retrieving user with ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Creates a new user.
     *
     * @param user the user data to be created
     * @return a ResponseEntity containing the created user or an internal server
     *         error status
     */
    @PostMapping
    public ResponseEntity<UserDTO> createUser(@Valid @RequestBody User user) {
        try {
            log.info("Attempting to create a user: {}", user.getName());
            if (user.getUsername().contains("admin")) {
                log.warn("Attempt to create admin user: {}", user.getUsername());
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            UserDTO savedUser = userService.saveUser(user);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedUser);
        } catch (Exception e) {
            log.error("Error creating user {}: {}", user.getName(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Deletes a user by their ID.
     *
     * @param id the ID of the user to delete
     * @return a ResponseEntity indicating the result of the deletion
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteUser(@PathVariable Long id) {
        Map<String, String> response = new HashMap<>();
        try {
            log.info("Attempting to delete user with ID: {}", id);
            Optional<UserDTO> user = userService.findUserById(id);

            if (user.isEmpty()) {
                log.warn("No user found with ID: {}", id);
                response.put("message", "User not found.");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            } else if (user.get().username().toLowerCase().contains("admin")) {
                log.warn("It is not possible to delete the default admin user.");
                response.put("message", "It is not possible to delete the default admin user.");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            } else {
                log.info("User found: {}", user.get());
                userService.deleteUser(id);
                response.put("message", "Successfully deleted user!");
                return ResponseEntity.ok(response);
            }

        } catch (Exception e) {
            log.error("Error deleting user with ID {}: {}", id, e.getMessage(), e);
            response.put("message", "Error deleting user: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

}
