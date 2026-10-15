package com.example.serviceApp.DTO;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TicketUpdateRequest {

    @Size(max = 100)
    private String title;

    @Size(max = 2000)
    private String description;

    @Size(max = 80)
    private String category;

}
