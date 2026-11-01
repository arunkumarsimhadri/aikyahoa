package com.aikya.domain.controller;

import com.aikya.common.ApiResponse;
import com.aikya.domain.dto.DashboardSummaryDto;
import com.aikya.domain.service.GovernanceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final GovernanceService governanceService;

    public DashboardController(GovernanceService governanceService) {
        this.governanceService = governanceService;
    }

    @GetMapping("/summary")
    public ApiResponse<DashboardSummaryDto> getDashboardSummary() {
        return ApiResponse.ok(governanceService.getDashboardSummary());
    }
}
