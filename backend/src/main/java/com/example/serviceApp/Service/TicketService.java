package com.example.serviceApp.Service;

import com.example.serviceApp.DTO.ChatMessageResponse;
import com.example.serviceApp.DTO.TicketRequest;
import com.example.serviceApp.DTO.TicketResponse;
import com.example.serviceApp.DTO.TicketUpdateRequest;
import com.example.serviceApp.Entity.*;
import com.example.serviceApp.Repository.TicketRepository;
import com.example.serviceApp.Repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class TicketService {

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;

    public TicketService(TicketRepository ticketRepository, UserRepository userRepository) {
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
    }

    public TicketResponse createTicket(TicketRequest request, CustomUserDetails user) {
        Ticket ticket = new Ticket();
        ticket.setTitle(request.getTitle().trim());
        ticket.setDescription(request.getDescription().trim());
        ticket.setCategory(request.getCategory().trim());
        ticket.setPriority(TicketPriority.MEDIUM);
        ticket.setStatus(TicketStatus.OPEN);
        ticket.setCreatedBy(user.getId());
        ticket.setCreatedAt(LocalDateTime.now());
        ticket.setUpdatedAt(LocalDateTime.now());

        return toResponse(ticketRepository.save(ticket));
    }

    public Page<TicketResponse> listTickets(
            CustomUserDetails user,
            int page,
            int size,
            TicketStatus status,
            TicketPriority priority,
            String search
    ) {
        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException("Page must be >= 0 and size must be between 1 and 100");
        }

        List<Ticket> tickets;
        if (user.getRole() == Role.ADMIN) {
            tickets = ticketRepository.findAllByOrderByUpdatedAtDesc();
        } else if (user.getRole() == Role.SUPPORT_AGENT) {
            tickets = ticketRepository.findByAssignedAgentIdOrderByUpdatedAtDesc(user.getId());
        } else {
            tickets = ticketRepository.findByCreatedByOrderByUpdatedAtDesc(user.getId());
        }

        String searchText = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);

        List<TicketResponse> filtered = tickets.stream()
                .filter(ticket -> status == null || ticket.getStatus() == status)
                .filter(ticket -> priority == null || ticket.getPriority() == priority)
                .filter(ticket -> searchText.isEmpty()
                        || ticket.getTitle().toLowerCase(Locale.ROOT).contains(searchText)
                        || ticket.getDescription().toLowerCase(Locale.ROOT).contains(searchText))
                .map(this::toResponse)
                .toList();

        Pageable pageable = PageRequest.of(page, size);
        int fromIndex = Math.min((int) pageable.getOffset(), filtered.size());
        int toIndex = Math.min(fromIndex + size, filtered.size());

        return new PageImpl<>(filtered.subList(fromIndex, toIndex), pageable, filtered.size());
    }

    public TicketResponse getTicket(String ticketId, CustomUserDetails user) {
        Ticket ticket = findTicket(ticketId);
        validateTicketAccess(ticket, user);
        return toResponse(ticket);
    }

    public TicketResponse updateTicket(
            String ticketId,
            TicketUpdateRequest request,
            CustomUserDetails user
    ) {
        Ticket ticket = findTicket(ticketId);
        validateTicketAccess(ticket, user);

        if (user.getRole() != Role.USER) {
            if (request.getTitle() != null && !request.getTitle().isBlank()) {
                ticket.setTitle(request.getTitle().trim());
            }
            if (request.getDescription() != null && !request.getDescription().isBlank()) {
                ticket.setDescription(request.getDescription().trim());
            }
            if (request.getCategory() != null && !request.getCategory().isBlank()) {
                ticket.setCategory(request.getCategory().trim());
            }
        } else {
            if (ticket.getStatus() != TicketStatus.OPEN) {
                throw new AccessDeniedException("Only open tickets can be edited by their creator");
            }
            if (request.getTitle() != null && !request.getTitle().isBlank()) {
                ticket.setTitle(request.getTitle().trim());
            }
            if (request.getDescription() != null && !request.getDescription().isBlank()) {
                ticket.setDescription(request.getDescription().trim());
            }
            if (request.getCategory() != null && !request.getCategory().isBlank()) {
                ticket.setCategory(request.getCategory().trim());
            }
        }

        ticket.setUpdatedAt(LocalDateTime.now());
        return toResponse(ticketRepository.save(ticket));
    }

    public TicketResponse updateStatus(String ticketId, TicketStatus status, CustomUserDetails user) {
        Ticket ticket = findTicket(ticketId);
        validateTicketAccess(ticket, user);

        if (user.getRole() == Role.USER && status != TicketStatus.CLOSED) {
            throw new AccessDeniedException("Users may only close their own tickets");
        }

        ticket.setStatus(status);
        ticket.setUpdatedAt(LocalDateTime.now());
        Ticket saved = ticketRepository.save(ticket);
        return toResponse(saved);
    }

    public TicketResponse updatePriority(String ticketId, TicketPriority priority) {
        Ticket ticket = findTicket(ticketId);
        ticket.setPriority(priority);
        ticket.setUpdatedAt(LocalDateTime.now());
        return toResponse(ticketRepository.save(ticket));
    }

    public TicketResponse assignAgent(String ticketId, String agentId) {
        Ticket ticket = findTicket(ticketId);
        User agent = userRepository.findById(agentId)
                .orElseThrow(() -> new IllegalArgumentException("Agent not found"));

        if (agent.getRole() != Role.SUPPORT_AGENT) {
            throw new IllegalArgumentException("Selected user is not a support agent");
        }

        ticket.setAssignedAgentId(agentId);
        ticket.setUpdatedAt(LocalDateTime.now());
        return toResponse(ticketRepository.save(ticket));
    }

    public List<ChatMessageResponse> getMessages(String ticketId, CustomUserDetails user,
                                                 com.example.serviceApp.Repository.ChatMessageRepository messages) {
        Ticket ticket = findTicket(ticketId);
        validateTicketAccess(ticket, user);

        return messages.findByTicketIdOrderByTimestampAsc(ticketId)
                .stream()
                .map(message -> new ChatMessageResponse(
                        message.getId(),
                        message.getTicketId(),
                        message.getSenderId(),
                        message.getSenderUsername(),
                        message.getContent(),
                        message.getTimestamp()
                ))
                .toList();
    }

    public Ticket findTicket(String ticketId) {
        return ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found"));
    }

    public void validateTicketAccess(Ticket ticket, CustomUserDetails user) {
        if (user.getRole() == Role.ADMIN) {
            return;
        }

        boolean isCreator = ticket.getCreatedBy().equals(user.getId());
        boolean isAssignedAgent = ticket.getAssignedAgentId() != null
                && ticket.getAssignedAgentId().equals(user.getId());

        if (!isCreator && !isAssignedAgent) {
            throw new AccessDeniedException("You are not allowed to access this ticket");
        }
    }

    private TicketResponse toResponse(Ticket ticket) {
        return new TicketResponse(
                ticket.getId(),
                ticket.getTitle(),
                ticket.getDescription(),
                ticket.getCategory(),
                ticket.getPriority(),
                ticket.getStatus(),
                ticket.getCreatedBy(),
                ticket.getAssignedAgentId(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt()
        );
    }
}
