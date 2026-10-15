package com.example.serviceApp.DTO;

import com.example.serviceApp.Entity.TicketStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StatusUpdateRequest {

    @NotNull
    private TicketStatus status;
}
