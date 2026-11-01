package com.aikya.domain.controller;

import com.aikya.common.ApiResponse;
import com.aikya.domain.dto.NoticeCreateRequest;
import com.aikya.domain.entity.Notice;
import com.aikya.domain.service.GovernanceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notices")
public class NoticeController {

    private final GovernanceService governanceService;

    public NoticeController(GovernanceService governanceService) {
        this.governanceService = governanceService;
    }

    @GetMapping
    public ApiResponse<List<Notice>> listNotices() {
        return ApiResponse.ok(governanceService.listNotices());
    }

    @PostMapping
    public ApiResponse<Notice> createNotice(@RequestBody NoticeCreateRequest request) {
        return ApiResponse.ok("Community notice published", governanceService.createNotice(request));
    }
}
