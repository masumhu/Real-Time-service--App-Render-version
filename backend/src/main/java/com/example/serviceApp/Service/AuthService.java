package com.example.serviceApp.Service;

import com.example.serviceApp.DTO.AuthResponse;
import com.example.serviceApp.DTO.LoginRequest;
import com.example.serviceApp.DTO.RegisterRequest;
import com.example.serviceApp.Entity.CustomUserDetails;
import com.example.serviceApp.Entity.Role;
import com.example.serviceApp.Entity.User;
import com.example.serviceApp.Repository.UserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CustomUserDetailsService userDetailsService;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            CustomUserDetailsService userDetailsService,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.userDetailsService = userDetailsService;
        this.jwtService = jwtService;
    }

    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.getEmail());

        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email is already registered");
        }

        User user = new User();
        user.setUsername(request.getUsername().trim());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.USER);

        User savedUser = userRepository.save(user);
        CustomUserDetails userDetails =
                (CustomUserDetails) userDetailsService.loadUserByUsername(savedUser.getEmail());

        return createResponse(userDetails);
    }

    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.getEmail());

        CustomUserDetails userDetails;
        try {
            userDetails = (CustomUserDetails) userDetailsService.loadUserByUsername(email);
        } catch (Exception exception) {
            throw new BadCredentialsException("Invalid email or password");
        }

        if (!passwordEncoder.matches(request.getPassword(), userDetails.getPassword())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        return createResponse(userDetails);
    }

    private AuthResponse createResponse(CustomUserDetails userDetails) {
        return new AuthResponse(
                jwtService.generateToken(userDetails),
                userDetails.getId(),
                userDetails.getDisplayName(),
                userDetails.getEmail(),
                userDetails.getRole()
        );
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
