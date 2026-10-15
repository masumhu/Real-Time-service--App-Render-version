package com.example.serviceApp.Controller;

import com.example.serviceApp.DTO.RoleUpdateRequest;
import com.example.serviceApp.Entity.Role;
import com.example.serviceApp.Entity.User;
import com.example.serviceApp.Repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserRepository userRepository;

    public AdminController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/users")
    public List<Map<String, Object>> getUsers() {
        return userRepository.findAll().stream()
                .map(user -> Map.<String, Object>of(
                        "id", user.getId(),
                        "username", user.getUsername(),
                        "email", user.getEmail(),
                        "role", user.getRole(),
                        "createdAt", user.getCreatedAt()
                ))
                .toList();
    }

    @GetMapping("/agents")
    public List<Map<String, Object>> getAgents() {
        return userRepository.findAll().stream()
                .filter(user -> user.getRole() == Role.SUPPORT_AGENT)
                .map(user -> Map.<String, Object>of(
                        "id", user.getId(),
                        "username", user.getUsername(),
                        "email", user.getEmail(),
                        "role", user.getRole()
                ))
                .toList();
    }

    @PatchMapping("/users/{userId}/role")
    public Map<String, Object> updateRole(
            @PathVariable String userId,
            @Valid @RequestBody RoleUpdateRequest request
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.setRole(request.getRole());
        User saved = userRepository.save(user);

        return Map.of(
                "id", saved.getId(),
                "username", saved.getUsername(),
                "email", saved.getEmail(),
                "role", saved.getRole()
        );
    }
}
