package ufsc.br.epibuilder.service;

import lombok.RequiredArgsConstructor;
import ufsc.br.epibuilder.dto.UserDTO;
import ufsc.br.epibuilder.model.User;
import ufsc.br.epibuilder.repository.UserRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

import java.util.List;
import java.util.Optional;

/**
 * Service class for managing user-related operations.
 * Provides methods for retrieving, saving, and deleting users.
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Retrieves all users from the database.
     *
     * @return List of UserDTOs representing all users.
     */
    public List<UserDTO> findAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
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
        // Encrypting the password before saving
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // Saving in the database
        User savedUser = userRepository.save(user);

        // Returning the saved user as DTO (without password)
        return toDto(savedUser);
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
        return new UserDTO(user.getId(), user.getName(), user.getUsername(), user.getRole(), null);
    }
}

