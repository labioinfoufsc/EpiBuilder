package ufsc.br.epibuilder.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import ufsc.br.epibuilder.dto.UserDTO;
import ufsc.br.epibuilder.exception.UserAlreadyExistsException;
import ufsc.br.epibuilder.model.Role;
import ufsc.br.epibuilder.model.User;
import ufsc.br.epibuilder.repository.UserRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
class UserServiceTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @MockBean
    private PasswordEncoder passwordEncoder;

    @MockBean
    private SecurityContext securityContext;

    @MockBean
    private Authentication authentication;

    private User testUser;
    private User adminUser;

    @BeforeEach
    void setUp() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
        when(authentication.getPrincipal()).thenReturn("admin");

        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");

        testUser = new User();
        testUser.setName("Test User");
        testUser.setUsername("testuser");
        testUser.setPassword("password");
        testUser.setRole(Role.USER);

        adminUser = new User();
        adminUser.setName("Admin User");
        adminUser.setUsername("admin");
        adminUser.setPassword("adminpass");
        adminUser.setRole(Role.ADMIN);

        userRepository.deleteAll();
        userRepository.save(adminUser);
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
    }

    @Test
    void getAllUsersExceptLoggedShouldReturnAllUsersExceptLogged() {
        userRepository.save(testUser);

        List<UserDTO> users = userService.getAllUsersExceptLogged();

        assertEquals(1, users.size());
        assertEquals("Test User", users.get(0).name());
        assertFalse(users.stream().anyMatch(u -> u.username().equals("admin")));
    }

    @Test
    void findUserByIdShouldReturnUserWhenExists() {
        User savedUser = userRepository.save(testUser);

        Optional<UserDTO> foundUser = userService.findUserById(savedUser.getId());

        assertTrue(foundUser.isPresent());
        assertEquals(savedUser.getId(), foundUser.get().id());
        assertEquals(savedUser.getUsername(), foundUser.get().username());
    }

    @Test
    void findUserByIdShouldReturnEmptyWhenUserNotExists() {
        Optional<UserDTO> foundUser = userService.findUserById(999L);

        assertFalse(foundUser.isPresent());
    }

    @Test
    void saveUserShouldSaveUserWithEncodedPassword() {
        UserDTO savedUser = userService.saveUser(testUser);

        assertNotNull(savedUser.id());
        assertEquals(testUser.getUsername(), savedUser.username());
        verify(passwordEncoder).encode("password");

        Optional<User> userInDb = userRepository.findById(savedUser.id());
        assertTrue(userInDb.isPresent());
        assertEquals("encodedPassword", userInDb.get().getPassword());
    }

    @Test
    void saveUserShouldThrowExceptionWhenUsernameExists() {
        userRepository.save(testUser);

        assertThrows(UserAlreadyExistsException.class, () -> {
            userService.saveUser(testUser);
        });
    }

    @Test
    void updateUserShouldUpdateUserData() {
        User savedUser = userRepository.save(testUser);
        User updateData = new User();
        updateData.setName("Updated Name");
        updateData.setUsername("updateduser");
        updateData.setPassword("newpassword");
        updateData.setRole(Role.ADMIN);

        Optional<UserDTO> updatedUser = userService.updateUser(savedUser.getId(), updateData);

        assertTrue(updatedUser.isPresent());
        assertEquals("Updated Name", updatedUser.get().name());
        assertEquals("updateduser", updatedUser.get().username());
        assertEquals(Role.ADMIN, updatedUser.get().role());

        verify(passwordEncoder).encode("newpassword");
    }

    @Test
    void updateUserShouldNotUpdatePasswordWhenBlank() {
        User savedUser = userRepository.save(testUser);
        User updateData = new User();
        updateData.setPassword("");

        Optional<UserDTO> updatedUser = userService.updateUser(savedUser.getId(), updateData);

        assertTrue(updatedUser.isPresent());
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void deleteUserShouldRemoveUserFromDatabase() {
        User savedUser = userRepository.save(testUser);

        userService.deleteUser(savedUser.getId());

        assertFalse(userRepository.existsById(savedUser.getId()));
    }

    @Test
    void findByUsernameShouldReturnUserWhenExists() {
        userRepository.save(testUser);

        Optional<User> foundUser = userService.findByUsername(testUser.getUsername());

        assertTrue(foundUser.isPresent());
        assertEquals(testUser.getUsername(), foundUser.get().getUsername());
    }
}