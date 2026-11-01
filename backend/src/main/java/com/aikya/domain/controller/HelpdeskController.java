package com.aikya.domain.controller;

import com.aikya.common.ApiResponse;
import com.aikya.domain.dto.TicketCreateRequest;
import com.aikya.domain.dto.TicketDto;
import com.aikya.domain.dto.TicketStatusUpdateRequest;
import com.aikya.domain.service.HelpdeskService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tickets")
public class HelpdeskController {

    private final HelpdeskService helpdeskService;

    public HelpdeskController(HelpdeskService helpdeskService) {
        this.helpdeskService = helpdeskService;
    }

    @GetMapping
    public ApiResponse<List<TicketDto>> listTickets(
            @RequestParam(required = false) String status
    ) {
        return ApiResponse.ok(helpdeskService.listTickets(status));
    }

    @PostMapping
    public ApiResponse<TicketDto> createTicket(@RequestBody TicketCreateRequest request) {
        return ApiResponse.ok("Helpdesk ticket raised with SLA timer", helpdeskService.createTicket(request));
    }

    @PatchMapping("/{ticketId}/status")
    public ApiResponse<TicketDto> patchTicketStatus(
            @PathVariable Long ticketId,
            @RequestBody TicketStatusUpdateRequest request
    ) {
        return ApiResponse.ok("Ticket status updated", helpdeskService.updateTicketStatus(ticketId, request));
    }

    @PutMapping("/{ticketId}/status")
    public ApiResponse<TicketDto> putTicketStatus(
            @PathVariable Long ticketId,
            @RequestBody TicketStatusUpdateRequest request
    ) {
        return ApiResponse.ok("Ticket status updated", helpdeskService.updateTicketStatus(ticketId, request));
    }

    @PostMapping("/{ticketId}/status")
    public ApiResponse<TicketDto> postTicketStatus(
            @PathVariable Long ticketId,
            @RequestBody TicketStatusUpdateRequest request
    ) {
        return ApiResponse.ok("Ticket status updated", helpdeskService.updateTicketStatus(ticketId, request));
    }
}
