package com.example.serviceApp.Entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Document(collection = "tickets")
public class Ticket {

    @Id
    private String id;

    private String title;

    private String description;

    private String category;

    private TicketPriority priority = TicketPriority.MEDIUM;

    private TicketStatus status = TicketStatus.OPEN;

    private String createdBy;

    private String assignedAgentId;

    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt = LocalDateTime.now();
}
