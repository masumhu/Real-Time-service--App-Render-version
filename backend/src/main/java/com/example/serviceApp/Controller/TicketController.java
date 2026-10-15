package com.example.serviceApp.Controller;

import com.example.serviceApp.DTO.*;
import com.example.serviceApp.Entity.CustomUserDetails;
import com.example.serviceApp.Entity.TicketPriority;
import com.example.serviceApp.Entity.TicketStatus;
import com.example.serviceApp.Service.ChatMessageService;
import com.example.serviceApp.Service.TicketService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService ticketService;
    private final ChatMessageService chatMessageService;

    public TicketController(TicketService ticketService, ChatMessageService chatMessageService) {
        this.ticketService = ticketService;
        this.chatMessageService = chatMessageService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('USER')")
    public TicketResponse createTicket(
            @Valid @RequestBody TicketRequest request,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        return ticketService.createTicket(request, user);
    }

    @GetMapping
    public Page<TicketResponse> listTickets(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(required = false) TicketPriority priority,
            @RequestParam(required = false) String search
    ) {
        return ticketService.listTickets(user, page, size, status, priority, search);
    }

    @GetMapping("/{ticketId}")
    public TicketResponse getTicket(
            @PathVariable String ticketId,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        return ticketService.getTicket(ticketId, user);
    }

    @PatchMapping("/{ticketId}")
    public TicketResponse updateTicket(
            @PathVariable String ticketId,
            @Valid @RequestBody TicketUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        return ticketService.updateTicket(ticketId, request, user);
    }

    @PatchMapping("/{ticketId}/status")
    public TicketResponse updateStatus(
            @PathVariable String ticketId,
            @Valid @RequestBody StatusUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        return ticketService.updateStatus(ticketId, request.getStatus(), user);
    }

    @PatchMapping("/{ticketId}/priority")
    @PreAuthorize("hasRole('ADMIN')")
    public TicketResponse updatePriority(
            @PathVariable String ticketId,
            @Valid @RequestBody PriorityUpdateRequest request
    ) {
        return ticketService.updatePriority(ticketId, request.getPriority());
    }

    @PostMapping("/{ticketId}/assign")
    @PreAuthorize("hasRole('ADMIN')")
    public TicketResponse assignAgent(
            @PathVariable String ticketId,
            @Valid @RequestBody AssignmentRequest request
    ) {
        return ticketService.assignAgent(ticketId, request.getAgentId());
    }

    @GetMapping("/{ticketId}/messages")
    public java.util.List<ChatMessageResponse> getMessages(
            @PathVariable String ticketId,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        return chatMessageService.getMessages(ticketId, user);
    }

    @PostMapping("/{ticketId}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public ChatMessageResponse sendMessage(
            @PathVariable String ticketId,
            @Valid @RequestBody ChatMessageRequest request,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        return chatMessageService.sendMessage(ticketId, request, user);
    }
}
