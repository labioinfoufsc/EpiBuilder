package ufsc.br.epibuilder.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import ufsc.br.epibuilder.model.Role;

/**
 * Data Transfer Object (DTO) representing a user.
 *
 * @param id       the unique identifier of the user
 * @param name     the name of the user (must not be empty)
 * @param username the username of the user (must not be empty, with a length
 *                 between 4 and 20 characters)
 * @param role     the role assigned to the user
 * @param token    the authentication token associated with the user
 */
public record UserDTO(
        Long id,
        @NotBlank(message = "Name cannot be empty") String name,
        @NotBlank(message = "Username cannot be empty") @Size(min = 4, max = 20) String username,
        Role role,
        String token) {
}
