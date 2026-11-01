package com.aikya.domain.service;

import com.aikya.common.AikyaException;
import com.aikya.common.AuthenticatedUser;
import com.aikya.common.NotificationPort;
import com.aikya.common.SecurityContextHelper;
import com.aikya.domain.dto.TicketCreateRequest;
import com.aikya.domain.dto.TicketDto;
import com.aikya.domain.dto.TicketStatusUpdateRequest;
import com.aikya.domain.entity.HelpdeskTicket;
import com.aikya.domain.entity.Unit;
import com.aikya.domain.repository.HelpdeskTicketRepository;
import com.aikya.domain.repository.UnitRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class HelpdeskService {

    private static final Map<String, Set<String>> VALID_TRANSITIONS = Map.of(
            "OPEN", Set.of("IN_PROGRESS", "RESOLVED"),
            "IN_PROGRESS", Set.of("RESOLVED", "CLOSED"),
            "RESOLVED", Set.of("CLOSED", "IN_PROGRESS"),
            "CLOSED", Set.of()
    );

    private final HelpdeskTicketRepository ticketRepository;
    private final UnitRepository unitRepository;
    private final ObjectProvider<NotificationPort> notificationPortProvider;

    public HelpdeskService(
            HelpdeskTicketRepository ticketRepository,
            UnitRepository unitRepository,
            ObjectProvider<NotificationPort> notificationPortProvider
    ) {
        this.ticketRepository = ticketRepository;
        this.unitRepository = unitRepository;
        this.notificationPortProvider = notificationPortProvider;
    }

    @Transactional
    public List<TicketDto> listTickets(String status) {
        List<HelpdeskTicket> tickets;
        if (status != null && !status.isBlank()) {
            tickets = ticketRepository.findByStatusOrderByCreatedAtDesc(status.trim().toUpperCase());
        } else {
            tickets = ticketRepository.findAllByOrderByCreatedAtDesc();
        }

        Instant now = Instant.now();
        for (HelpdeskTicket ticket : tickets) {
            boolean activeStatus = "OPEN".equalsIgnoreCase(ticket.getStatus())
                    || "IN_PROGRESS".equalsIgnoreCase(ticket.getStatus());
            if (activeStatus && ticket.getSlaDueAt() != null && now.isAfter(ticket.getSlaDueAt()) && !ticket.isSlaBreached()) {
                ticket.setSlaBreached(true);
                ticketRepository.save(ticket);
            }
        }

        return tickets.stream().map(this::toTicketDto).toList();
    }

    @Transactional
    public TicketDto createTicket(TicketCreateRequest request) {
        if (request == null || request.title() == null || request.title().isBlank()) {
            throw new AikyaException(HttpStatus.BAD_REQUEST, "Ticket title is required");
        }
        if (request.description() == null || request.description().isBlank()) {
            throw new AikyaException(HttpStatus.BAD_REQUEST, "Ticket description is required");
        }

        AuthenticatedUser currentUser = SecurityContextHelper.getCurrentUser();
        String priority = (request.priority() != null && !request.priority().isBlank())
                ? request.priority().trim().toUpperCase()
                : "MEDIUM";
        String category = (request.category() != null && !request.category().isBlank())
                ? request.category().trim().toUpperCase()
                : "GENERAL";

        Long resolvedUnitId = request.unitId();
        String resolvedUnitNumber = request.unitNumber() != null && !request.unitNumber().isBlank()
                ? request.unitNumber().trim()
                : currentUser.unitNumber();

        if (resolvedUnitId == null && resolvedUnitNumber != null) {
            resolvedUnitId = unitRepository.findByUnitNumber(resolvedUnitNumber)
                    .map(Unit::getId)
                    .orElse(null);
        } else if (resolvedUnitId != null && resolvedUnitNumber == null) {
            resolvedUnitNumber = unitRepository.findById(resolvedUnitId)
                    .map(Unit::getUnitNumber)
                    .orElse("COMMON");
        }

        Instant now = Instant.now();
        Instant slaDueAt = calculateSlaDueAt(now, priority);

        HelpdeskTicket ticket = new HelpdeskTicket();
        ticket.setTicketNumber("TKT-" + (10000 + (System.currentTimeMillis() % 89999)));
        ticket.setUnitId(resolvedUnitId);
        ticket.setUnitNumber(resolvedUnitNumber != null ? resolvedUnitNumber : "COMMON");
        ticket.setRaisedByUserId(currentUser.userId());
        ticket.setRaisedByName(currentUser.fullName());
        ticket.setCategory(category);
        ticket.setPriority(priority);
        ticket.setTitle(request.title().trim());
        ticket.setDescription(request.description().trim());
        ticket.setAttachmentUrl(request.attachmentUrl());
        ticket.setStatus("OPEN");
        ticket.setSlaDueAt(slaDueAt);
        ticket.setSlaBreached(false);
        ticket.setCreatedAt(now);
        ticket.setUpdatedAt(now);

        HelpdeskTicket saved = ticketRepository.save(ticket);

        NotificationPort notificationPort = notificationPortProvider.getIfAvailable();
        if (notificationPort != null && currentUser.phoneNumber() != null) {
            notificationPort.sendNotification(
                    currentUser.userId(),
                    currentUser.phoneNumber(),
                    currentUser.fullName(),
                    currentUser.preferredLocale() != null ? currentUser.preferredLocale() : "en",
                    "SMS",
                    "TICKET_STATUS_UPDATED",
                    Map.of(
                            "ticketNumber", saved.getTicketNumber(),
                            "category", saved.getCategory(),
                            "status", saved.getStatus(),
                            "technician", "Unassigned"
                    )
            );
        }

        return toTicketDto(saved);
    }

    @Transactional
    public TicketDto updateTicketStatus(Long ticketId, TicketStatusUpdateRequest request) {
        if (request == null || request.status() == null || request.status().isBlank()) {
            throw new AikyaException(HttpStatus.BAD_REQUEST, "Target status is required");
        }

        HelpdeskTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new AikyaException(HttpStatus.NOT_FOUND, "Helpdesk ticket not found: " + ticketId));

        String currentStatus = ticket.getStatus().toUpperCase();
        String targetStatus = request.status().trim().toUpperCase();

        if (!currentStatus.equals(targetStatus)) {
            Set<String> allowed = VALID_TRANSITIONS.getOrDefault(currentStatus, Set.of());
            if (!allowed.contains(targetStatus)) {
                throw new AikyaException(HttpStatus.BAD_REQUEST,
                        "Invalid ticket status transition from " + currentStatus + " to " + targetStatus);
            }
        }

        Instant now = Instant.now();
        ticket.setStatus(targetStatus);
        if (request.assignedToUserId() != null) {
            ticket.setAssignedToUserId(request.assignedToUserId());
        }
        if (request.assignedTechnicianName() != null && !request.assignedTechnicianName().isBlank()) {
            ticket.setAssignedTechnicianName(request.assignedTechnicianName().trim());
        }
        if (request.assignedTechnicianPhone() != null && !request.assignedTechnicianPhone().isBlank()) {
            ticket.setAssignedTechnicianPhone(request.assignedTechnicianPhone().trim());
        }
        if (request.resolutionNotes() != null && !request.resolutionNotes().isBlank()) {
            ticket.setResolutionNotes(request.resolutionNotes().trim());
        }

        if ("RESOLVED".equals(targetStatus)) {
            ticket.setResolvedAt(now);
        } else if ("CLOSED".equals(targetStatus)) {
            if (ticket.getResolvedAt() == null) {
                ticket.setResolvedAt(now);
            }
            ticket.setClosedAt(now);
        }

        boolean active = "OPEN".equals(targetStatus) || "IN_PROGRESS".equals(targetStatus);
        if (active && ticket.getSlaDueAt() != null && now.isAfter(ticket.getSlaDueAt())) {
            ticket.setSlaBreached(true);
        }
        ticket.setUpdatedAt(now);

        HelpdeskTicket saved = ticketRepository.save(ticket);

        AuthenticatedUser actor = SecurityContextHelper.getCurrentUserOrNull();
        NotificationPort notificationPort = notificationPortProvider.getIfAvailable();
        if (notificationPort != null && actor != null && actor.phoneNumber() != null) {
            notificationPort.sendNotification(
                    saved.getRaisedByUserId(),
                    actor.phoneNumber(),
                    saved.getRaisedByName() != null ? saved.getRaisedByName() : "Resident",
                    actor.preferredLocale() != null ? actor.preferredLocale() : "en",
                    "SMS",
                    "TICKET_STATUS_UPDATED",
                    Map.of(
                            "ticketNumber", saved.getTicketNumber(),
                            "category", saved.getCategory(),
                            "status", saved.getStatus(),
                            "technician", saved.getAssignedTechnicianName() != null ? saved.getAssignedTechnicianName() : "Assigned Staff"
                    )
            );
        }

        return toTicketDto(saved);
    }

    private Instant calculateSlaDueAt(Instant createdAt, String priority) {
        return switch (priority) {
            case "CRITICAL" -> createdAt.plus(4, ChronoUnit.HOURS);
            case "HIGH" -> createdAt.plus(12, ChronoUnit.HOURS);
            case "LOW" -> createdAt.plus(48, ChronoUnit.HOURS);
            default -> createdAt.plus(24, ChronoUnit.HOURS);
        };
    }

    public TicketDto toTicketDto(HelpdeskTicket t) {
        boolean breached = t.isSlaBreached() || (
                ("OPEN".equalsIgnoreCase(t.getStatus()) || "IN_PROGRESS".equalsIgnoreCase(t.getStatus()))
                        && t.getSlaDueAt() != null
                        && Instant.now().isAfter(t.getSlaDueAt())
        );
        return new TicketDto(
                t.getId(),
                t.getTicketNumber(),
                t.getUnitId(),
                t.getUnitNumber(),
                t.getRaisedByUserId(),
                t.getRaisedByName(),
                t.getCategory(),
                t.getPriority(),
                t.getTitle(),
                t.getDescription(),
                t.getAttachmentUrl(),
                t.getStatus(),
                t.getAssignedToUserId(),
                t.getAssignedTechnicianName(),
                t.getAssignedTechnicianPhone(),
                t.getSlaDueAt(),
                breached,
                t.getResolutionNotes(),
                t.getResolvedAt(),
                t.getClosedAt(),
                t.getCreatedAt(),
                t.getUpdatedAt()
        );
    }
}
