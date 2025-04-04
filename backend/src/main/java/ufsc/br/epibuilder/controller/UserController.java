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

/**
 * Controller responsible for handling user-related operations.
 */
@RestController
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
     * @return a ResponseEntity with no content if successful or an internal server
     *         error status
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        try {
            log.info("Attempting to delete user with ID: {}", id);
            userService.deleteUser(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Error deleting user with ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
