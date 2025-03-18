package ufsc.br.epibuilder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

import org.springframework.security.crypto.password.PasswordEncoder;

import ufsc.br.epibuilder.dto.UserDTO;
import ufsc.br.epibuilder.model.Role;
import ufsc.br.epibuilder.model.User;
import ufsc.br.epibuilder.repository.UserRepository;
import ufsc.br.epibuilder.service.UserService;

/**
 * Unit tests for the UserService class.
 */
@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User user1;
    private User user2;
    private User user3;

    @BeforeEach
    void setUp() {
        user1 = new User(1L, "John Smith", "johnsmith", "123456", Role.USER);
        user2 = new User(2L, "Michael Johnson", "michaelj", "michaelj", Role.USER);
        user3 = new User(3L, "David Williams", "davidw", "david1234", Role.ADMIN);
    }

    /**
     * Test to verify that findAllUsers returns a list containing exactly three
     * users.
     */
    @Test
    void testFindAllUsers() {
        List<User> users = Arrays.asList(user1, user2, user3);
        when(userRepository.findAll()).thenReturn(users);

        List<UserDTO> result = userService.findAllUsers();

        assertEquals(3, result.size());
        assertEquals("John Smith", result.get(0).name());
        assertEquals("Michael Johnson", result.get(1).name());
        assertEquals("David Williams", result.get(2).name());
    }

    /**
     * Test to verify that an existing user is found by ID.
     */
    @Test
    void testFindUserById_ExistingUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));

        Optional<UserDTO> result = userService.findUserById(1L);

        assertTrue(result.isPresent());
        assertEquals(1L, result.get().id());
        assertEquals("John Smith", result.get().name());
    }

    /**
     * Test to verify that a non-existing user returns an empty result.
     */
    @Test
    void testFindUserById_NonExistingUser() {
        when(userRepository.findById(4L)).thenReturn(Optional.empty());

        Optional<UserDTO> result = userService.findUserById(4L);

        assertFalse(result.isPresent());
    }

    /**
     * Test to verify that a user is saved correctly with encrypted password.
     */
    @Test
    void testSaveUser() {
        when(passwordEncoder.encode("michaelj")).thenReturn("encryptedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            savedUser.setId(2L); // Simulate database-generated ID
            return savedUser;
        });

        User savedUser = new User(null, "Michael Johnson", "michaelj", "michaelj", Role.USER);
        UserDTO savedUserDTO = userService.saveUser(savedUser);

        assertNotNull(savedUserDTO);
        assertEquals("Michael Johnson", savedUserDTO.name());

        // Ensure password was encrypted before saving
        verify(passwordEncoder, times(1)).encode("michaelj");
    }

    /**
     * Test to verify that a user is deleted correctly.
     */
    @Test
    void testDeleteUser() {
        doNothing().when(userRepository).deleteById(1L);

        userService.deleteUser(1L);

        verify(userRepository, times(1)).deleteById(1L);
    }
}
