package com.example.serviceApp.DTO;

import java.time.LocalDateTime;

public record ChatMessageResponse(
        String id,
        String ticketId,
        String senderId,
        String senderUsername,
        String content,
        LocalDateTime timestamp
) {
}
