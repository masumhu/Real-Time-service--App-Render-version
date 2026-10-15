package com.example.serviceApp.DTO;

import com.example.serviceApp.Entity.TicketPriority;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PriorityUpdateRequest {

    @NotNull
    private TicketPriority priority;
}
