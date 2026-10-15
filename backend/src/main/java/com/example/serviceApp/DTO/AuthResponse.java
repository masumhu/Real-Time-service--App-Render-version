package com.example.serviceApp.DTO;

import com.example.serviceApp.Entity.Role;

public record AuthResponse(
        String token,
        String id,
        String username,
        String email,
        Role role
) {
}
