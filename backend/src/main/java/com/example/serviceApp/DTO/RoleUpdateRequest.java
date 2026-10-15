package com.example.serviceApp.DTO;

import com.example.serviceApp.Entity.Role;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RoleUpdateRequest {

    @NotNull
    private Role role;
}
