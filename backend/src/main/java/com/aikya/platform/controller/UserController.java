package com.aikya.platform.controller;

import com.aikya.common.ApiResponse;
import com.aikya.platform.dto.UpdateLocaleRequest;
import com.aikya.platform.dto.UserDto;
import com.aikya.platform.service.AuthService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final AuthService authService;

    public UserController(AuthService authService) {
        this.authService = authService;
    }

    @RequestMapping(value = "/locale", method = {RequestMethod.PATCH, RequestMethod.PUT, RequestMethod.POST})
    public ApiResponse<UserDto> updateLocale(@RequestBody UpdateLocaleRequest request) {
        String requestedLocale = request != null ? request.resolvedLocale() : "en";
        UserDto updated = authService.updatePreferredLocale(requestedLocale);
        return ApiResponse.ok("Preferred locale updated", updated);
    }

    @GetMapping("/me")
    public ApiResponse<UserDto> getMe() {
        return ApiResponse.ok(authService.getCurrentUserProfile());
    }
}
