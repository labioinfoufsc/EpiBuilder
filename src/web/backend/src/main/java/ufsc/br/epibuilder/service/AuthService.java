package ufsc.br.epibuilder.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import ufsc.br.epibuilder.dto.UserDTO;
import ufsc.br.epibuilder.model.User;
import ufsc.br.epibuilder.repository.UserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;

import java.security.Key;
import java.util.Date;
import java.util.function.Function;

/**
 * Service responsible for handling authentication and token generation.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;

    @Value("${jwt.secret}")
    private String secretKey;

    /**
     * Authenticates a user and generates a JWT token upon successful
     * authentication.
     *
     * @param request the user credentials for authentication
     * @return a {@link UserDTO} containing user details and the generated token
     * @throws RuntimeException if authentication fails or token generation
     *                          encounters an error
     */
    public UserDTO authenticate(User request) {
        try {
            log.info("Attempting authentication for user: {}", request.getUsername());
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
            log.info("Authentication successful");

            log.info("Searching user in database: {}", request.getUsername());
            User user = userRepository.findByUsername(request.getUsername())
                    .orElseThrow(() -> {
                        log.error("User not found: {}", request.getUsername());
                        return new RuntimeException("User not found");
                    });
            log.info("User found: {}", user.getId());

            // Salvar o usuário logado no SecurityContextHolder
            Authentication authentication = new UsernamePasswordAuthenticationToken(user.getUsername(), null,
                    user.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authentication);

            try {
                log.info("Generating token for user: {}", user.getId());
                String token = generateToken(user);
                log.info("Token generated successfully");
                return new UserDTO(user.getId(), user.getName(), user.getUsername(), user.getEpitopeTaskDataList(),
                        user.getRole(), token);
            } catch (Exception tokenException) {
                log.error("Error generating token for user: {}", user.getId(), tokenException);
                throw new RuntimeException("Error generating token");
            }
        } catch (org.springframework.security.authentication.BadCredentialsException e) {
            log.warn("Invalid credentials for user: {}", request.getUsername());
            throw new RuntimeException("Invalid username or password");
        } catch (RuntimeException e) {
            log.error("Authentication failed: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during authentication", e);
            throw new RuntimeException("Internal server error");
        }
    }

    public User getLoggedInUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            String username = authentication.getName();
            return userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));
        } else {
            throw new RuntimeException("No user is logged in");
        }
    }

    /**
     * Generates a JWT token for an authenticated user.
     *
     * @param user the authenticated user
     * @return the generated JWT token
     */
    private String generateToken(User user) {
        return Jwts.builder()
                .setSubject(user.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 10)) // 10 hours
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Retrieves the signing key used for token generation.
     *
     * @return the signing key
     */
    private Key getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Extracts the username from a given JWT token.
     *
     * @param token the JWT token
     * @return the extracted username
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extracts a specific claim from the JWT token.
     *
     * @param token          the JWT token
     * @param claimsResolver a function to resolve the desired claim
     * @param <T>            the type of the claim
     * @return the extracted claim value
     */
    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claimsResolver.apply(claims);
    }
}
