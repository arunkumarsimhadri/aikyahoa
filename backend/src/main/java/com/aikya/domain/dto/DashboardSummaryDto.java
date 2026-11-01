package com.aikya.domain.dto;

import com.aikya.domain.entity.Notice;

import java.math.BigDecimal;
import java.util.List;

public record DashboardSummaryDto(
        BigDecimal totalOutstandingDues,
        long pendingInvoicesCount,
        long unreconciledPaymentsCount,
        BigDecimal totalCollectionThisMonth,
        long openTicketsCount,
        long slaBreachedTicketsCount,
        long upcomingBookingsCount,
        long activePollsCount,
        List<InvoiceDto> recentInvoices,
        List<TicketDto> activeTickets,
        List<Notice> recentNotices
) {
}
