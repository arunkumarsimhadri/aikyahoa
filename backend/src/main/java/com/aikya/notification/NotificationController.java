package com.aikya.notification;

import com.aikya.common.ApiResponse;
import com.aikya.common.AuthenticatedUser;
import com.aikya.common.SecurityContextHelper;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final LocalizedNotificationService notificationService;

    public NotificationController(LocalizedNotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/logs")
    public ApiResponse<List<NotificationLogDto>> getNotificationLogs() {
        return ApiResponse.ok(notificationService.getRecentLogs());
    }

    @PostMapping("/preview")
    public ApiResponse<NotificationLogDto> previewNotification(@RequestBody NotificationPreviewRequest request) {
        AuthenticatedUser currentUser = SecurityContextHelper.getCurrentUserOrNull();
        Long userId = currentUser != null ? currentUser.userId() : null;
        String recipientPhone = (request.recipientPhone() != null && !request.recipientPhone().isBlank())
                ? request.recipientPhone()
                : (currentUser != null ? currentUser.phoneNumber() : "+919848012345");
        String recipientName = (request.recipientName() != null && !request.recipientName().isBlank())
                ? request.recipientName()
                : (currentUser != null ? currentUser.fullName() : "Resident");
        String locale = (request.locale() != null && !request.locale().isBlank())
                ? request.locale()
                : (currentUser != null ? currentUser.preferredLocale() : "en");

        NotificationLogDto logDto = notificationService.dispatchAndLog(
                userId,
                recipientPhone,
                recipientName,
                locale,
                request.channel(),
                request.templateKey(),
                request.resolvedVariables()
        );
        return ApiResponse.ok(logDto.renderedMessage(), logDto);
    }
}
