package com.aikya.domain.service;

import com.aikya.common.AikyaException;
import com.aikya.common.AuthenticatedUser;
import com.aikya.common.NotificationPort;
import com.aikya.common.SecurityContextHelper;
import com.aikya.domain.dto.DashboardSummaryDto;
import com.aikya.domain.dto.DocumentCreateRequest;
import com.aikya.domain.dto.InvoiceDto;
import com.aikya.domain.dto.NoticeCreateRequest;
import com.aikya.domain.dto.PollCreateRequest;
import com.aikya.domain.dto.PollCreateRequest.PollOptionInput;
import com.aikya.domain.dto.PollDto;
import com.aikya.domain.dto.PollOptionDto;
import com.aikya.domain.dto.PollVoteRequest;
import com.aikya.domain.dto.TicketDto;
import com.aikya.domain.entity.Document;
import com.aikya.domain.entity.Notice;
import com.aikya.domain.entity.Payment;
import com.aikya.domain.entity.Poll;
import com.aikya.domain.entity.PollOption;
import com.aikya.domain.entity.PollVote;
import com.aikya.domain.entity.Unit;
import com.aikya.domain.repository.AmenityBookingRepository;
import com.aikya.domain.repository.DocumentRepository;
import com.aikya.domain.repository.NoticeRepository;
import com.aikya.domain.repository.PaymentRepository;
import com.aikya.domain.repository.PollOptionRepository;
import com.aikya.domain.repository.PollRepository;
import com.aikya.domain.repository.PollVoteRepository;
import com.aikya.domain.repository.UnitRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class GovernanceService {

    private final PollRepository pollRepository;
    private final PollOptionRepository pollOptionRepository;
    private final PollVoteRepository pollVoteRepository;
    private final DocumentRepository documentRepository;
    private final NoticeRepository noticeRepository;
    private final UnitRepository unitRepository;
    private final PaymentRepository paymentRepository;
    private final AmenityBookingRepository amenityBookingRepository;
    private final BillingService billingService;
    private final HelpdeskService helpdeskService;
    private final ObjectProvider<NotificationPort> notificationPortProvider;

    public GovernanceService(
            PollRepository pollRepository,
            PollOptionRepository pollOptionRepository,
            PollVoteRepository pollVoteRepository,
            DocumentRepository documentRepository,
            NoticeRepository noticeRepository,
            UnitRepository unitRepository,
            PaymentRepository paymentRepository,
            AmenityBookingRepository amenityBookingRepository,
            BillingService billingService,
            HelpdeskService helpdeskService,
            ObjectProvider<NotificationPort> notificationPortProvider
    ) {
        this.pollRepository = pollRepository;
        this.pollOptionRepository = pollOptionRepository;
        this.pollVoteRepository = pollVoteRepository;
        this.documentRepository = documentRepository;
        this.noticeRepository = noticeRepository;
        this.unitRepository = unitRepository;
        this.paymentRepository = paymentRepository;
        this.amenityBookingRepository = amenityBookingRepository;
        this.billingService = billingService;
        this.helpdeskService = helpdeskService;
        this.notificationPortProvider = notificationPortProvider;
    }

    @Transactional(readOnly = true)
    public List<PollDto> listPolls() {
        AuthenticatedUser currentUser = SecurityContextHelper.getCurrentUserOrNull();
        Long currentUnitId = resolveCurrentUserUnitId(currentUser);
        return pollRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(poll -> toPollDto(poll, currentUnitId))
                .toList();
    }

    @Transactional
    public PollDto createPoll(PollCreateRequest request) {
        if (request == null || request.title() == null || request.title().isBlank()) {
            throw new AikyaException(HttpStatus.BAD_REQUEST, "Poll title is required");
        }
        AuthenticatedUser currentUser = SecurityContextHelper.getCurrentUser();
        if (!currentUser.role().isManagingCommittee()) {
            throw new AikyaException(HttpStatus.FORBIDDEN, "Only Managing Committee members can create resolutions/polls");
        }

        Instant now = Instant.now();
        Poll poll = new Poll();
        poll.setTitle(request.title().trim());
        poll.setTitleTe(request.titleTe());
        poll.setDescription(request.description());
        poll.setDescriptionTe(request.descriptionTe());
        poll.setSecret(request.isSecret());
        poll.setOwnerOnly(request.ownerOnly());
        poll.setStatus("ACTIVE");
        poll.setStartsAt(now);
        poll.setEndsAt(request.endsAt() != null ? request.endsAt() : now.plus(7, ChronoUnit.DAYS));
        poll.setCreatedByUserId(currentUser.userId());
        poll.setCreatedByName(currentUser.fullName());
        poll.setCreatedAt(now);

        Poll savedPoll = pollRepository.save(poll);

        List<PollOptionInput> inputs = (request.options() != null && !request.options().isEmpty())
                ? request.options()
                : List.of(
                        new PollOptionInput("Approve Resolution", "తీర్మానాన్ని ఆమోదించండి"),
                        new PollOptionInput("Reject Resolution", "తీర్మానాన్ని తిరస్కరించండి"),
                        new PollOptionInput("Abstain", "తటస్థం")
                );

        int order = 1;
        for (PollOptionInput input : inputs) {
            if (input.optionText() == null || input.optionText().isBlank()) {
                continue;
            }
            PollOption option = new PollOption();
            option.setPollId(savedPoll.getId());
            option.setOptionText(input.optionText().trim());
            option.setOptionTextTe(input.optionTextTe());
            option.setDisplayOrder(order++);
            pollOptionRepository.save(option);
        }

        Long currentUnitId = resolveCurrentUserUnitId(currentUser);
        return toPollDto(savedPoll, currentUnitId);
    }

    @Transactional
    public PollDto castVote(Long pollId, PollVoteRequest request) {
        if (request == null || request.optionId() == null) {
            throw new AikyaException(HttpStatus.BAD_REQUEST, "Option ID is required to cast a vote");
        }

        Poll poll = pollRepository.findById(pollId)
                .orElseThrow(() -> new AikyaException(HttpStatus.NOT_FOUND, "Poll not found: " + pollId));

        if (!"ACTIVE".equalsIgnoreCase(poll.getStatus())) {
            throw new AikyaException(HttpStatus.BAD_REQUEST, "This poll is no longer active");
        }

        AuthenticatedUser currentUser = SecurityContextHelper.getCurrentUser();
        if (poll.isOwnerOnly() && !currentUser.role().canVoteInOwnerPoll()) {
            throw new AikyaException(HttpStatus.FORBIDDEN,
                    "Only unit OWNER roles are eligible to vote in this resolution");
        }

        PollOption selectedOption = pollOptionRepository.findById(request.optionId())
                .orElseThrow(() -> new AikyaException(HttpStatus.NOT_FOUND, "Poll option not found: " + request.optionId()));
        if (!selectedOption.getPollId().equals(poll.getId())) {
            throw new AikyaException(HttpStatus.BAD_REQUEST, "Selected option does not belong to this poll");
        }

        Long unitId = request.unitId();
        String unitNumber = (request.unitNumber() != null && !request.unitNumber().isBlank())
                ? request.unitNumber().trim()
                : currentUser.unitNumber();

        if (unitId == null && unitNumber != null) {
            Optional<Unit> found = unitRepository.findByUnitNumber(unitNumber);
            if (found.isPresent()) {
                unitId = found.get().getId();
                unitNumber = found.get().getUnitNumber();
            }
        }
        if (unitId == null) {
            Unit fallbackUnit = unitRepository.findAllByOrderByBlockNameAscUnitNumberAsc().stream()
                    .findFirst()
                    .orElseThrow(() -> new AikyaException(HttpStatus.BAD_REQUEST, "No unit found to associate with vote"));
            unitId = fallbackUnit.getId();
            if (unitNumber == null) {
                unitNumber = fallbackUnit.getUnitNumber();
            }
        } else if (unitNumber == null) {
            unitNumber = unitRepository.findById(unitId)
                    .map(Unit::getUnitNumber)
                    .orElse("A-101");
        }

        if (pollVoteRepository.existsByPollIdAndUnitId(pollId, unitId)) {
            throw new AikyaException(HttpStatus.CONFLICT,
                    "A vote has already been cast for unit " + unitNumber);
        }

        PollVote vote = new PollVote();
        vote.setPollId(pollId);
        vote.setOptionId(selectedOption.getId());
        vote.setUnitId(unitId);
        vote.setUnitNumber(unitNumber);
        vote.setVoterUserId(currentUser.userId());
        vote.setVoterName(currentUser.fullName());
        vote.setVotedAt(Instant.now());

        pollVoteRepository.save(vote);
        return toPollDto(poll, unitId);
    }

    @Transactional(readOnly = true)
    public List<Document> listDocuments(String category) {
        if (category != null && !category.isBlank()) {
            return documentRepository.findByCategoryOrderByCreatedAtDesc(category.trim().toUpperCase());
        }
        return documentRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional
    public Document createDocument(DocumentCreateRequest request) {
        if (request == null || request.title() == null || request.title().isBlank()) {
            throw new AikyaException(HttpStatus.BAD_REQUEST, "Document title is required");
        }
        AuthenticatedUser currentUser = SecurityContextHelper.getCurrentUser();
        if (!currentUser.role().isManagingCommittee()) {
            throw new AikyaException(HttpStatus.FORBIDDEN, "Only Managing Committee members can upload AGM/Governance documents");
        }

        Document doc = new Document();
        doc.setTitle(request.title().trim());
        doc.setTitleTe(request.titleTe());
        doc.setCategory(request.category() != null && !request.category().isBlank()
                ? request.category().trim().toUpperCase()
                : "AGM_MINUTES");
        doc.setFinancialYear(request.financialYear() != null && !request.financialYear().isBlank()
                ? request.financialYear().trim()
                : "2025-26");
        doc.setFileUrl(request.fileUrl() != null && !request.fileUrl().isBlank()
                ? request.fileUrl().trim()
                : "/api/v1/storage/files/aikya_assoc_1/agm-resolution.pdf");
        doc.setFileSizeBytes(request.fileSizeBytes() != null && request.fileSizeBytes() > 0
                ? request.fileSizeBytes()
                : 245760L);
        doc.setUploadedByUserId(currentUser.userId());
        doc.setUploadedByName(currentUser.fullName());
        doc.setCreatedAt(Instant.now());

        return documentRepository.save(doc);
    }

    @Transactional(readOnly = true)
    public List<Notice> listNotices() {
        return noticeRepository.findAllByOrderByPinnedDescPublishedAtDesc();
    }

    @Transactional
    public Notice createNotice(NoticeCreateRequest request) {
        if (request == null || request.title() == null || request.title().isBlank()) {
            throw new AikyaException(HttpStatus.BAD_REQUEST, "Notice title is required");
        }
        if (request.content() == null || request.content().isBlank()) {
            throw new AikyaException(HttpStatus.BAD_REQUEST, "Notice content is required");
        }

        AuthenticatedUser currentUser = SecurityContextHelper.getCurrentUser();
        if (!currentUser.role().isManagingCommittee()) {
            throw new AikyaException(HttpStatus.FORBIDDEN, "Only Managing Committee members can publish community notices");
        }

        Notice notice = new Notice();
        notice.setTitle(request.title().trim());
        notice.setTitleTe(request.titleTe());
        notice.setContent(request.content().trim());
        notice.setContentTe(request.contentTe());
        notice.setCategory(request.category() != null && !request.category().isBlank()
                ? request.category().trim().toUpperCase()
                : "GENERAL");
        notice.setPriority(request.priority() != null && !request.priority().isBlank()
                ? request.priority().trim().toUpperCase()
                : "NORMAL");
        notice.setPinned(request.pinned());
        notice.setPublishedByUserId(currentUser.userId());
        notice.setPublishedByName(currentUser.fullName());
        notice.setPublishedAt(Instant.now());
        notice.setExpiresAt(request.expiresAt());

        Notice saved = noticeRepository.save(notice);

        NotificationPort notificationPort = notificationPortProvider.getIfAvailable();
        if (notificationPort != null && currentUser.phoneNumber() != null) {
            notificationPort.sendNotification(
                    currentUser.userId(),
                    currentUser.phoneNumber(),
                    currentUser.fullName(),
                    currentUser.preferredLocale() != null ? currentUser.preferredLocale() : "en",
                    "WHATSAPP",
                    "notice.published",
                    Map.of(
                            "title", saved.getTitle(),
                            "category", saved.getCategory()
                    )
            );
        }

        return saved;
    }

    @Transactional
    public DashboardSummaryDto getDashboardSummary() {
        List<InvoiceDto> allInvoices = billingService.listInvoices(null, null);
        BigDecimal totalOutstandingDues = allInvoices.stream()
                .filter(inv -> !"PAID".equalsIgnoreCase(inv.status()))
                .map(InvoiceDto::balanceDue)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        long pendingInvoicesCount = allInvoices.stream()
                .filter(inv -> !"PAID".equalsIgnoreCase(inv.status()))
                .count();

        List<Payment> allPayments = paymentRepository.findAllByOrderByCreatedAtDesc();
        long unreconciledPaymentsCount = allPayments.stream()
                .filter(p -> "PENDING_VERIFICATION".equalsIgnoreCase(p.getReconciliationStatus()))
                .count();

        BigDecimal totalCollectionThisMonth = allPayments.stream()
                .filter(p -> "VERIFIED".equalsIgnoreCase(p.getReconciliationStatus()))
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        List<TicketDto> allTickets = helpdeskService.listTickets(null);
        long openTicketsCount = allTickets.stream()
                .filter(t -> "OPEN".equalsIgnoreCase(t.status()) || "IN_PROGRESS".equalsIgnoreCase(t.status()))
                .count();

        long slaBreachedTicketsCount = allTickets.stream()
                .filter(TicketDto::slaBreached)
                .count();

        long upcomingBookingsCount = amenityBookingRepository
                .findByBookingDateGreaterThanEqualAndStatusOrderByBookingDateAscStartTimeAsc(LocalDate.now(), "CONFIRMED")
                .size();

        long activePollsCount = pollRepository.findByStatusOrderByCreatedAtDesc("ACTIVE").size();

        List<InvoiceDto> recentInvoices = allInvoices.stream().limit(6).toList();
        List<TicketDto> activeTickets = allTickets.stream()
                .filter(t -> !"CLOSED".equalsIgnoreCase(t.status()))
                .limit(6)
                .toList();
        List<Notice> recentNotices = noticeRepository.findAllByOrderByPinnedDescPublishedAtDesc()
                .stream()
                .limit(5)
                .toList();

        return new DashboardSummaryDto(
                totalOutstandingDues,
                pendingInvoicesCount,
                unreconciledPaymentsCount,
                totalCollectionThisMonth,
                openTicketsCount,
                slaBreachedTicketsCount,
                upcomingBookingsCount,
                activePollsCount,
                recentInvoices,
                activeTickets,
                recentNotices
        );
    }

    private Long resolveCurrentUserUnitId(AuthenticatedUser user) {
        if (user == null || user.unitNumber() == null || user.unitNumber().isBlank()) {
            return null;
        }
        return unitRepository.findByUnitNumber(user.unitNumber().trim())
                .map(Unit::getId)
                .orElse(null);
    }

    private PollDto toPollDto(Poll poll, Long currentUnitId) {
        List<PollOption> options = pollOptionRepository.findByPollIdOrderByDisplayOrderAsc(poll.getId());
        List<PollVote> votes = pollVoteRepository.findByPollId(poll.getId());
        long totalVotes = votes.size();

        Map<Long, List<PollVote>> votesByOption = votes.stream()
                .collect(Collectors.groupingBy(PollVote::getOptionId));

        Long votedOptionId = null;
        if (currentUnitId != null) {
            votedOptionId = votes.stream()
                    .filter(v -> currentUnitId.equals(v.getUnitId()))
                    .map(PollVote::getOptionId)
                    .findFirst()
                    .orElse(null);
        }

        List<PollOptionDto> optionDtos = new ArrayList<>();
        for (PollOption opt : options) {
            List<PollVote> optVotes = votesByOption.getOrDefault(opt.getId(), List.of());
            long count = optVotes.size();
            double pct = totalVotes > 0
                    ? Math.round((count * 1000.0) / totalVotes) / 10.0
                    : 0.0;

            List<String> votersList;
            if (poll.isSecret()) {
                votersList = List.of();
            } else {
                votersList = optVotes.stream()
                        .map(v -> "Unit " + v.getUnitNumber() + " (" + (v.getVoterName() != null ? v.getVoterName() : "Owner") + ")")
                        .toList();
            }

            optionDtos.add(new PollOptionDto(
                    opt.getId(),
                    opt.getOptionText(),
                    opt.getOptionTextTe(),
                    opt.getDisplayOrder(),
                    count,
                    pct,
                    votersList
            ));
        }

        return new PollDto(
                poll.getId(),
                poll.getTitle(),
                poll.getTitleTe(),
                poll.getDescription(),
                poll.getDescriptionTe(),
                poll.isSecret(),
                poll.isOwnerOnly(),
                poll.getStatus(),
                poll.getStartsAt(),
                poll.getEndsAt(),
                poll.getCreatedByName(),
                totalVotes,
                votedOptionId != null,
                votedOptionId,
                optionDtos,
                poll.getCreatedAt()
        );
    }
}
