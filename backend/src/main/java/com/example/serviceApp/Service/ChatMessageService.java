package com.example.serviceApp.Service;

import com.example.serviceApp.DTO.ChatMessageRequest;
import com.example.serviceApp.DTO.ChatMessageResponse;
import com.example.serviceApp.Entity.ChatMessage;
import com.example.serviceApp.Entity.CustomUserDetails;
import com.example.serviceApp.Entity.Ticket;
import com.example.serviceApp.Entity.TicketStatus;
import com.example.serviceApp.Repository.ChatMessageRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class ChatMessageService {

    private final ChatMessageRepository messageRepository;
    private final TicketService ticketService;

    public ChatMessageService(ChatMessageRepository messageRepository, TicketService ticketService) {
        this.messageRepository = messageRepository;
        this.ticketService = ticketService;
    }

    public ChatMessageResponse sendMessage(
            String ticketId,
            ChatMessageRequest request,
            CustomUserDetails sender
    ) {
        Ticket ticket = ticketService.findTicket(ticketId);
        ticketService.validateTicketAccess(ticket, sender);

        if (ticket.getStatus() == TicketStatus.CLOSED) {
            throw new IllegalArgumentException("Cannot send messages to a closed ticket");
        }

        ChatMessage message = new ChatMessage();
        message.setTicketId(ticketId);
        message.setSenderId(sender.getId());
        message.setSenderUsername(sender.getDisplayName());
        message.setContent(request.getContent().trim());
        message.setTimestamp(LocalDateTime.now());

        return toResponse(messageRepository.save(message));
    }

    public java.util.List<ChatMessageResponse> getMessages(String ticketId, CustomUserDetails user) {
        Ticket ticket = ticketService.findTicket(ticketId);
        ticketService.validateTicketAccess(ticket, user);

        return messageRepository.findByTicketIdOrderByTimestampAsc(ticketId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private ChatMessageResponse toResponse(ChatMessage message) {
        return new ChatMessageResponse(
                message.getId(),
                message.getTicketId(),
                message.getSenderId(),
                message.getSenderUsername(),
                message.getContent(),
                message.getTimestamp()
        );
    }
}
