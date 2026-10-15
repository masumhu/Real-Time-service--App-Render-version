package com.example.serviceApp.DTO;

import com.example.serviceApp.Entity.TicketPriority;
import com.example.serviceApp.Entity.TicketStatus;

import java.time.LocalDateTime;

public record TicketResponse(
        String id,
        String title,
        String description,
        String category,
        TicketPriority priority,
        TicketStatus status,
        String createdBy,
        String assignedAgentId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
