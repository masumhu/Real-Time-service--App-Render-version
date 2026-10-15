package com.example.serviceApp.Controller;

import com.example.serviceApp.DTO.ChatMessageRequest;
import com.example.serviceApp.DTO.ChatMessageResponse;
import com.example.serviceApp.Entity.CustomUserDetails;
import com.example.serviceApp.Entity.Ticket;
import com.example.serviceApp.Entity.TicketStatus;
import com.example.serviceApp.Service.ChatMessageService;
import com.example.serviceApp.Service.TicketService;
import jakarta.validation.Valid;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;

import java.security.Principal;
import java.util.Map;

@Controller
public class WebSocketChatController {

    private final ChatMessageService chatMessageService;
    private final TicketService ticketService;
    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketChatController(
            ChatMessageService chatMessageService,
            TicketService ticketService,
            SimpMessagingTemplate messagingTemplate
    ) {
        this.chatMessageService = chatMessageService;
        this.ticketService = ticketService;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/tickets/{ticketId}/message")
    public void sendMessage(
            @DestinationVariable String ticketId,
            ChatMessageRequest request,
            Principal principal
    ) {
        CustomUserDetails sender = getUser(principal);
        ChatMessageResponse saved = chatMessageService.sendMessage(ticketId, request, sender);
        messagingTemplate.convertAndSend("/topic/tickets/" + ticketId, saved);
    }

    @MessageMapping("/tickets/{ticketId}/typing")
    public void typing(
            @DestinationVariable String ticketId,
            Map<String, Boolean> payload,
            Principal principal
    ) {
        CustomUserDetails sender = getUser(principal);
        Ticket ticket = ticketService.findTicket(ticketId);
        ticketService.validateTicketAccess(ticket, sender);

        messagingTemplate.convertAndSend(
                "/topic/tickets/" + ticketId + "/typing",
                Map.of("userId", sender.getId(),
                        "username", sender.getDisplayName(),
                        "typing", Boolean.TRUE.equals(payload.get("typing")))
        );
    }

    private CustomUserDetails getUser(Principal principal) {
        if (principal instanceof org.springframework.security.authentication.UsernamePasswordAuthenticationToken token
                && token.getPrincipal() instanceof CustomUserDetails user) {
            return user;
        }

        throw new AccessDeniedException("WebSocket authentication is required");
    }
}
