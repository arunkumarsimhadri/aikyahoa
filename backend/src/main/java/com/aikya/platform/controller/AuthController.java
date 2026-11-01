package com.aikya.platform.controller;

import com.aikya.common.ApiResponse;
import com.aikya.platform.dto.AuthTokenResponse;
import com.aikya.platform.dto.OtpSendRequest;
import com.aikya.platform.dto.OtpSendResponse;
import com.aikya.platform.dto.OtpVerifyRequest;
import com.aikya.platform.dto.SwitchContextRequest;
import com.aikya.platform.dto.UserDto;
import com.aikya.platform.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/otp/send")
    public ApiResponse<OtpSendResponse> sendOtp(@Valid @RequestBody OtpSendRequest request) {
        OtpSendResponse response = authService.sendOtp(request);
        return ApiResponse.ok("OTP dispatched successfully", response);
    }

    @PostMapping("/otp/verify")
    public ApiResponse<AuthTokenResponse> verifyOtp(@Valid @RequestBody OtpVerifyRequest request) {
        AuthTokenResponse response = authService.verifyOtp(request);
        return ApiResponse.ok("OTP verified successfully", response);
    }

    @PostMapping("/switch-context")
    public ApiResponse<AuthTokenResponse> switchContext(@RequestBody SwitchContextRequest request) {
        AuthTokenResponse response = authService.switchContext(request);
        return ApiResponse.ok("Context switched successfully", response);
    }

    @GetMapping("/me")
    public ApiResponse<UserDto> getCurrentUser() {
        UserDto user = authService.getCurrentUserProfile();
        return ApiResponse.ok(user);
    }
}
