package ufsc.br.epibuilder.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ufsc.br.epibuilder.dto.UserDTO;
import ufsc.br.epibuilder.exception.UserAlreadyExistsException;
import ufsc.br.epibuilder.model.User;
import ufsc.br.epibuilder.repository.UserRepository;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.stream.Collectors;

import java.util.List;
import java.util.Optional;

/**
 * Service class for managing user-related operations.
 * Provides methods for retrieving, saving, and deleting users.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Retrieves all users from the database except the currently logged user.
     *
     * @return List of UserDTOs representing all users.
     */
    @GetMapping
    public List<UserDTO> getAllUsersExceptLogged() {
        String loggedUsername = getLoggedUsername();
        log.info("Listing all users except logged user: {}", loggedUsername);
        return userRepository.findAll().stream()
                .filter(user -> !user.getUsername().equals(loggedUsername))
                .map(user -> new UserDTO(user.getId(), user.getName(), user.getUsername(),
                        user.getEpitopeTaskDataList(), user.getRole(), null))
                .collect(Collectors.toList());
    }

    private String getLoggedUsername() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserDetails) {
            return ((UserDetails) principal).getUsername();
        } else {
            return principal.toString();
        }
    }

    /**
     * Retrieves a user by their ID.
     *
     * @param id The ID of the user to be retrieved.
     * @return An Optional containing the UserDTO if found, otherwise empty.
     */
    public Optional<UserDTO> findUserById(Long id) {
        return userRepository.findById(id).map(this::toDto);
    }

    /**
     * Creates and saves a new user, ensuring the password is encrypted before
     * storage.
     *
     * @param user The User entity containing user details.
     * @return The saved UserDTO.
     */
    public UserDTO saveUser(User user) {
        if (!userRepository.findByUsername(user.getUsername()).isEmpty()) {
            throw new UserAlreadyExistsException("Username already exists!");
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        User savedUser = userRepository.save(user);
        return toDto(savedUser);
    }

    /**
     * Updates an existing user.
     *
     * @param id   the ID of the user to update
     * @param user the updated user data
     * @return an Optional containing the updated user, or an empty Optional if the
     *         user was not found
     */
    public Optional<UserDTO> updateUser(Long id, User user) {
        user.setName(user.getName());
        user.setUsername(user.getUsername());
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRole(user.getRole());
        User updatedUser = userRepository.save(user);
        return Optional.of(toDto(updatedUser));
    }

    /**
     * Deletes a user by their ID.
     *
     * @param id The ID of the user to be deleted.
     */
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    /**
     * Finds a user by their username.
     *
     * @param username The username of the user.
     * @return An Optional containing the User if found, otherwise empty.
     */
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    /**
     * Converts a User entity to a UserDTO (excludes password).
     *
     * @param user The User entity.
     * @return The UserDTO.
     */
    private UserDTO toDto(User user) {
        return new UserDTO(user.getId(), user.getName(), user.getUsername(), user.getEpitopeTaskDataList(),
                user.getRole(), null);
    }
}
