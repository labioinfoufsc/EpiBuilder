package ufsc.br.epibuilder.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ufsc.br.epibuilder.model.User;

import java.util.Optional;

/**
 * Repository interface for managing {@link User} entities.
 * Extends {@link JpaRepository} to provide CRUD operations.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Finds a user by their username.
     *
     * @param username the username to search for
     * @return an {@link Optional} containing the user if found, otherwise empty
     */
    Optional<User> findByUsername(String username);

    /**
     * Deletes a user by their username.
     *
     * @param username the username of the user to delete
     */
    void deleteByUsername(String username);

    void deleteAll();

    /**
     * Checks if a user with the given username exists.
     *
     * @param username the username to check
     * @return true if the user exists, false otherwise
     */

    Optional<User> findById(Long id); // This method is already provided by JpaRepository
}
