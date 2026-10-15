package com.example.serviceApp.Entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Document(collection = "chat_messages")
public class ChatMessage {

    @Id
    private String id;

    private String ticketId;

    private String senderId;

    private String senderUsername;

    private String content;

    private LocalDateTime timestamp = LocalDateTime.now();
}
