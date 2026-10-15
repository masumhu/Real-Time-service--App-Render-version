package com.example.serviceApp.Repository;

import com.example.serviceApp.Entity.Ticket;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface TicketRepository extends MongoRepository<Ticket, String> {

    List<Ticket> findByCreatedByOrderByUpdatedAtDesc(String createdBy);

    List<Ticket> findByAssignedAgentIdOrderByUpdatedAtDesc(String assignedAgentId);

    List<Ticket> findAllByOrderByUpdatedAtDesc();
}
