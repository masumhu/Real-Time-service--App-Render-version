package com.example.serviceApp.Repository;

import com.example.serviceApp.Entity.ChatMessage;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ChatMessageRepository extends MongoRepository<ChatMessage, String> {

    List<ChatMessage> findByTicketIdOrderByTimestampAsc(String ticketId);
}
