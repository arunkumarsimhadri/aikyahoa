package com.aikya.domain.controller;

import com.aikya.common.ApiResponse;
import com.aikya.domain.dto.DocumentCreateRequest;
import com.aikya.domain.dto.PollCreateRequest;
import com.aikya.domain.dto.PollDto;
import com.aikya.domain.dto.PollVoteRequest;
import com.aikya.domain.entity.Document;
import com.aikya.domain.service.GovernanceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/governance")
public class GovernanceController {

    private final GovernanceService governanceService;

    public GovernanceController(GovernanceService governanceService) {
        this.governanceService = governanceService;
    }

    @GetMapping("/polls")
    public ApiResponse<List<PollDto>> listPolls() {
        return ApiResponse.ok(governanceService.listPolls());
    }

    @PostMapping("/polls")
    public ApiResponse<PollDto> createPoll(@RequestBody PollCreateRequest request) {
        return ApiResponse.ok("Resolution poll published", governanceService.createPoll(request));
    }

    @PostMapping("/polls/{pollId}/vote")
    public ApiResponse<PollDto> castVote(
            @PathVariable Long pollId,
            @RequestBody PollVoteRequest request
    ) {
        return ApiResponse.ok("Vote recorded for unit", governanceService.castVote(pollId, request));
    }

    @GetMapping("/documents")
    public ApiResponse<List<Document>> listDocuments(
            @RequestParam(required = false) String category
    ) {
        return ApiResponse.ok(governanceService.listDocuments(category));
    }

    @PostMapping("/documents")
    public ApiResponse<Document> createDocument(@RequestBody DocumentCreateRequest request) {
        return ApiResponse.ok("Governance document archived", governanceService.createDocument(request));
    }
}
