package com.example.serviceApp.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TicketRequest {

    @NotBlank
    @Size(max = 100)
    private String title;

    @NotBlank
    @Size(max = 2000)
    private String description;

    @NotBlank
    @Size(max = 80)
    private String category;

}
