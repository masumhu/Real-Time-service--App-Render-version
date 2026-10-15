package com.example.serviceApp.DTO;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AssignmentRequest {

    @NotBlank
    private String agentId;
}
