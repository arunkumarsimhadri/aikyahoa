package com.aikya.platform.service;

import com.aikya.common.AikyaException;
import com.aikya.common.AuthenticatedUser;
import com.aikya.common.NotificationPort;
import com.aikya.common.Role;
import com.aikya.common.SecurityContextHelper;
import com.aikya.common.TenantContext;
import com.aikya.platform.dto.AuthTokenResponse;
import com.aikya.platform.dto.MembershipDto;
import com.aikya.platform.dto.OtpSendRequest;
import com.aikya.platform.dto.OtpSendResponse;
import com.aikya.platform.dto.OtpVerifyRequest;
import com.aikya.platform.dto.SwitchContextRequest;
import com.aikya.platform.dto.UserDto;
import com.aikya.platform.entity.Association;
import com.aikya.platform.entity.AssociationMembership;
import com.aikya.platform.entity.OtpVerification;
import com.aikya.platform.entity.User;
import com.aikya.platform.repository.AssociationMembershipRepository;
import com.aikya.platform.repository.AssociationRepository;
import com.aikya.platform.repository.OtpVerificationRepository;
import com.aikya.platform.repository.UserRepository;
import com.aikya.platform.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final String DEFAULT_MOCK_OTP = "123456";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final AssociationRepository associationRepository;
    private final AssociationMembershipRepository membershipRepository;
    private final OtpVerificationRepository otpVerificationRepository;
    private final SmsGatewayProvider smsGatewayProvider;
    private final ObjectProvider<NotificationPort> notificationPortProvider;
    private final JwtService jwtService;
    private final long otpExpirationSeconds;
    private final boolean mockOtpEnabled;

    public AuthService(
            UserRepository userRepository,
            AssociationRepository associationRepository,
            AssociationMembershipRepository membershipRepository,
            OtpVerificationRepository otpVerificationRepository,
            SmsGatewayProvider smsGatewayProvider,
            ObjectProvider<NotificationPort> notificationPortProvider,
            JwtService jwtService,
            @Value("${aikya.security.otp-expiration-seconds:300}") long otpExpirationSeconds,
            @Value("${aikya.security.mock-otp-enabled:true}") boolean mockOtpEnabled
    ) {
        this.userRepository = userRepository;
        this.associationRepository = associationRepository;
        this.membershipRepository = membershipRepository;
        this.otpVerificationRepository = otpVerificationRepository;
        this.smsGatewayProvider = smsGatewayProvider;
        this.notificationPortProvider = notificationPortProvider;
        this.jwtService = jwtService;
        this.otpExpirationSeconds = otpExpirationSeconds;
        this.mockOtpEnabled = mockOtpEnabled;
    }

    @Transactional
    public OtpSendResponse sendOtp(OtpSendRequest request) {
        if (request == null || request.phoneNumber() == null || request.phoneNumber().isBlank()) {
            throw new AikyaException(HttpStatus.BAD_REQUEST, "Phone number is required");
        }

        String normalizedPhone = normalizePhoneNumber(request.phoneNumber());
        String otpCode = mockOtpEnabled
                ? DEFAULT_MOCK_OTP
                : String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));

        OtpVerification otp = new OtpVerification();
        otp.setPhoneNumber(normalizedPhone);
        otp.setOtpCode(otpCode);
        otp.setExpiresAt(Instant.now().plusSeconds(otpExpirationSeconds));
        otp.setVerified(false);
        otp.setAttempts(0);
        otpVerificationRepository.save(otp);

        Optional<User> existingUser = findUserFlexible(normalizedPhone, request.phoneNumber());
        String locale = existingUser.map(User::getPreferredLocale).orElse("en");

        smsGatewayProvider.sendOtpSms(normalizedPhone, otpCode, locale);

        NotificationPort notificationPort = notificationPortProvider.getIfAvailable();
        if (notificationPort != null && !TenantContext.DEFAULT_TENANT.equals(TenantContext.getCurrentTenant())) {
            try {
                notificationPort.sendNotification(
                        existingUser.map(User::getId).orElse(null),
                        normalizedPhone,
                        existingUser.map(User::getFullName).orElse("Resident"),
                        locale,
                        "SMS",
                        "OTP_LOGIN",
                        Map.of("otp", otpCode, "minutes", String.valueOf(otpExpirationSeconds / 60))
                );
            } catch (Exception ex) {
                log.debug("Optional NotificationPort OTP log skipped: {}", ex.getMessage());
            }
        }

        return new OtpSendResponse(
                normalizedPhone,
                otpExpirationSeconds,
                mockOtpEnabled ? otpCode : null
        );
    }

    @Transactional
    public AuthTokenResponse verifyOtp(OtpVerifyRequest request) {
        if (request == null || request.phoneNumber() == null || request.phoneNumber().isBlank()) {
            throw new AikyaException(HttpStatus.BAD_REQUEST, "Phone number is required");
        }
        String otpCode = request.resolvedOtpCode();
        if (otpCode.isBlank()) {
            throw new AikyaException(HttpStatus.BAD_REQUEST, "OTP code is required");
        }

        String normalizedPhone = normalizePhoneNumber(request.phoneNumber());
        Optional<OtpVerification> latestOtpOpt = otpVerificationRepository
                .findFirstByPhoneNumberAndVerifiedFalseOrderByCreatedAtDesc(normalizedPhone);

        if (latestOtpOpt.isEmpty() && !normalizedPhone.equals(request.phoneNumber().trim())) {
            latestOtpOpt = otpVerificationRepository
                    .findFirstByPhoneNumberAndVerifiedFalseOrderByCreatedAtDesc(request.phoneNumber().trim());
        }

        if (latestOtpOpt.isPresent()) {
            OtpVerification otpRecord = latestOtpOpt.get();
            otpRecord.setAttempts(otpRecord.getAttempts() + 1);

            if (otpRecord.getExpiresAt().isBefore(Instant.now())) {
                otpVerificationRepository.save(otpRecord);
                throw new AikyaException(HttpStatus.UNAUTHORIZED, "OTP has expired. Please request a new OTP.");
            }

            boolean matches = otpRecord.getOtpCode().equals(otpCode)
                    || (mockOtpEnabled && DEFAULT_MOCK_OTP.equals(otpCode));

            if (!matches) {
                otpVerificationRepository.save(otpRecord);
                throw new AikyaException(HttpStatus.UNAUTHORIZED, "Invalid OTP code");
            }

            otpRecord.setVerified(true);
            otpVerificationRepository.save(otpRecord);
        } else if (!(mockOtpEnabled && DEFAULT_MOCK_OTP.equals(otpCode))) {
            throw new AikyaException(HttpStatus.UNAUTHORIZED, "Invalid or expired OTP");
        }

        User user = findUserFlexible(normalizedPhone, request.phoneNumber())
                .orElseGet(() -> autoRegisterUser(normalizedPhone, request.associationId()));

        return buildAuthTokenResponse(user, request.associationId(), null, null);
    }

    @Transactional(readOnly = true)
    public AuthTokenResponse switchContext(SwitchContextRequest request) {
        AuthenticatedUser currentUser = SecurityContextHelper.getCurrentUserOrNull();
        User targetUser = null;

        if (request != null && request.userId() != null) {
            targetUser = userRepository.findById(request.userId())
                    .orElseThrow(() -> new AikyaException(HttpStatus.NOT_FOUND, "User not found: " + request.userId()));
        } else if (request != null && request.phoneNumber() != null && !request.phoneNumber().isBlank()) {
            String normalizedPhone = normalizePhoneNumber(request.phoneNumber());
            targetUser = findUserFlexible(normalizedPhone, request.phoneNumber())
                    .orElseThrow(() -> new AikyaException(
                            HttpStatus.NOT_FOUND,
                            "User not found with phone: " + request.phoneNumber()
                    ));
        } else if (currentUser != null && currentUser.userId() != null) {
            targetUser = userRepository.findById(currentUser.userId())
                    .orElseThrow(() -> new AikyaException(HttpStatus.NOT_FOUND, "Current user not found"));
        } else {
            throw new AikyaException(HttpStatus.UNAUTHORIZED, "Authentication or target persona required to switch context");
        }

        Long requestedAssocId = request != null ? request.associationId() : null;
        Long requestedMembershipId = request != null ? request.membershipId() : null;
        Role requestedRole = request != null ? request.role() : null;

        return buildAuthTokenResponse(targetUser, requestedAssocId, requestedMembershipId, requestedRole);
    }

    @Transactional(readOnly = true)
    public UserDto getCurrentUserProfile() {
        AuthenticatedUser currentUser = SecurityContextHelper.getCurrentUser();
        User user = userRepository.findById(currentUser.userId())
                .orElseThrow(() -> new AikyaException(HttpStatus.NOT_FOUND, "User not found"));

        List<MembershipDto> memberships = loadMembershipDtos(user.getId());
        MembershipDto activeMembership = selectActiveMembership(
                memberships,
                currentUser.associationId(),
                null,
                currentUser.role()
        );

        return toUserDto(user, activeMembership, memberships, currentUser);
    }

    @Transactional
    public UserDto updatePreferredLocale(String requestedLocale) {
        AuthenticatedUser currentUser = SecurityContextHelper.getCurrentUser();
        User user = userRepository.findById(currentUser.userId())
                .orElseThrow(() -> new AikyaException(HttpStatus.NOT_FOUND, "User not found"));

        String normalizedLocale = normalizeLocale(requestedLocale);
        user.setPreferredLocale(normalizedLocale);
        User saved = userRepository.save(user);

        List<MembershipDto> memberships = loadMembershipDtos(saved.getId());
        MembershipDto activeMembership = selectActiveMembership(
                memberships,
                currentUser.associationId(),
                null,
                currentUser.role()
        );

        return toUserDto(saved, activeMembership, memberships, currentUser);
    }

    private AuthTokenResponse buildAuthTokenResponse(
            User user,
            Long requestedAssociationId,
            Long requestedMembershipId,
            Role requestedRole
    ) {
        List<MembershipDto> memberships = loadMembershipDtos(user.getId());
        MembershipDto activeMembership = selectActiveMembership(
                memberships,
                requestedAssociationId,
                requestedMembershipId,
                requestedRole
        );

        Long activeAssociationId = activeMembership != null ? activeMembership.associationId() : null;
        String activeSchemaName = activeMembership != null
                ? activeMembership.schemaName()
                : TenantContext.DEFAULT_TENANT;
        Role activeRole = activeMembership != null
                ? activeMembership.role()
                : (requestedRole != null ? requestedRole : Role.OWNER);
        String activeUnitNumber = activeMembership != null ? activeMembership.unitNumber() : null;
        String activeBlockName = activeMembership != null ? activeMembership.blockName() : null;

        AuthenticatedUser authUser = new AuthenticatedUser(
                user.getId(),
                user.getPhoneNumber(),
                user.getFullName(),
                user.getPreferredLocale() != null ? user.getPreferredLocale() : "en",
                activeAssociationId,
                activeSchemaName,
                activeRole,
                activeUnitNumber,
                activeBlockName
        );

        String jwt = jwtService.generateToken(authUser);
        UserDto userDto = toUserDto(user, activeMembership, memberships, authUser);

        return new AuthTokenResponse(
                jwt,
                jwt,
                "Bearer",
                jwtService.getExpirationSeconds(),
                activeSchemaName,
                activeAssociationId,
                userDto,
                memberships
        );
    }

    private List<MembershipDto> loadMembershipDtos(Long userId) {
        List<AssociationMembership> memberships = membershipRepository.findByUserIdAndActiveTrueOrderByIdAsc(userId);
        if (memberships.isEmpty()) {
            return List.of();
        }
        Map<Long, Association> associationMap = associationRepository.findAllById(
                memberships.stream().map(AssociationMembership::getAssociationId).collect(Collectors.toSet())
        ).stream().collect(Collectors.toMap(Association::getId, Function.identity()));

        List<MembershipDto> result = new ArrayList<>();
        for (AssociationMembership m : memberships) {
            Association assoc = associationMap.get(m.getAssociationId());
            if (assoc != null && assoc.isActive()) {
                result.add(new MembershipDto(
                        m.getId(),
                        m.getId(),
                        assoc.getId(),
                        assoc.getName(),
                        assoc.getNameTe(),
                        assoc.getCode(),
                        assoc.getSchemaName(),
                        m.getRole(),
                        m.getUnitNumber(),
                        m.getBlockName()
                ));
            }
        }
        return result;
    }

    private MembershipDto selectActiveMembership(
            List<MembershipDto> memberships,
            Long requestedAssociationId,
            Long requestedMembershipId,
            Role requestedRole
    ) {
        if (memberships.isEmpty()) {
            return null;
        }
        if (requestedMembershipId != null) {
            for (MembershipDto m : memberships) {
                if (requestedMembershipId.equals(m.membershipId())) {
                    return m;
                }
            }
        }
        if (requestedAssociationId != null && requestedRole != null) {
            for (MembershipDto m : memberships) {
                if (requestedAssociationId.equals(m.associationId()) && requestedRole == m.role()) {
                    return m;
                }
            }
        }
        if (requestedAssociationId != null) {
            for (MembershipDto m : memberships) {
                if (requestedAssociationId.equals(m.associationId())) {
                    return m;
                }
            }
        }
        if (requestedRole != null) {
            for (MembershipDto m : memberships) {
                if (requestedRole == m.role()) {
                    return m;
                }
            }
        }
        return memberships.get(0);
    }

    private UserDto toUserDto(
            User user,
            MembershipDto activeMembership,
            List<MembershipDto> memberships,
            AuthenticatedUser fallbackContext
    ) {
        Role activeRole = activeMembership != null
                ? activeMembership.role()
                : (fallbackContext != null && fallbackContext.role() != null ? fallbackContext.role() : Role.OWNER);
        Long activeAssociationId = activeMembership != null
                ? activeMembership.associationId()
                : (fallbackContext != null ? fallbackContext.associationId() : null);
        String activeSchemaName = activeMembership != null
                ? activeMembership.schemaName()
                : (fallbackContext != null && fallbackContext.tenantSchema() != null
                        ? fallbackContext.tenantSchema()
                        : TenantContext.DEFAULT_TENANT);
        String activeUnitNumber = activeMembership != null
                ? activeMembership.unitNumber()
                : (fallbackContext != null ? fallbackContext.unitNumber() : null);
        String activeBlockName = activeMembership != null
                ? activeMembership.blockName()
                : (fallbackContext != null ? fallbackContext.blockName() : null);

        return new UserDto(
                user.getId(),
                user.getPhoneNumber(),
                user.getFullName(),
                user.getFullNameTe(),
                user.getEmail(),
                user.getPreferredLocale() != null ? user.getPreferredLocale() : "en",
                activeRole,
                activeAssociationId,
                activeSchemaName,
                activeUnitNumber,
                activeBlockName,
                memberships
        );
    }

    private Optional<User> findUserFlexible(String normalizedPhone, String rawPhone) {
        Optional<User> found = userRepository.findByPhoneNumber(normalizedPhone);
        if (found.isPresent()) {
            return found;
        }
        if (rawPhone != null && !rawPhone.isBlank()) {
            String trimmed = rawPhone.trim();
            if (!trimmed.equals(normalizedPhone)) {
                found = userRepository.findByPhoneNumber(trimmed);
                if (found.isPresent()) {
                    return found;
                }
            }
        }
        if (normalizedPhone.startsWith("+91") && normalizedPhone.length() == 13) {
            String tenDigit = normalizedPhone.substring(3);
            return userRepository.findByPhoneNumber(tenDigit);
        }
        return Optional.empty();
    }

    private User autoRegisterUser(String normalizedPhone, Long requestedAssociationId) {
        User newUser = new User();
        newUser.setPhoneNumber(normalizedPhone);
        String suffix = normalizedPhone.length() >= 4
                ? normalizedPhone.substring(normalizedPhone.length() - 4)
                : normalizedPhone;
        newUser.setFullName("Resident " + suffix);
        newUser.setFullNameTe("నివాసి " + suffix);
        newUser.setPreferredLocale("en");
        newUser.setActive(true);
        User savedUser = userRepository.saveAndFlush(newUser);

        List<Association> activeAssociations = associationRepository.findByActiveTrueOrderByIdAsc();
        if (!activeAssociations.isEmpty()) {
            Association targetAssoc = activeAssociations.get(0);
            if (requestedAssociationId != null) {
                for (Association assoc : activeAssociations) {
                    if (requestedAssociationId.equals(assoc.getId())) {
                        targetAssoc = assoc;
                        break;
                    }
                }
            }
            AssociationMembership membership = new AssociationMembership();
            membership.setUserId(savedUser.getId());
            membership.setAssociationId(targetAssoc.getId());
            membership.setRole(Role.OWNER);
            membership.setUnitNumber("A-101");
            membership.setBlockName("Block A");
            membership.setActive(true);
            membershipRepository.saveAndFlush(membership);
        }

        return savedUser;
    }

    public static String normalizePhoneNumber(String phoneNumber) {
        if (phoneNumber == null) {
            return "";
        }
        String digitsAndPlus = phoneNumber.trim().replaceAll("[\\s\\-()]", "");
        if (digitsAndPlus.matches("^\\d{10}$")) {
            return "+91" + digitsAndPlus;
        }
        if (digitsAndPlus.matches("^91\\d{10}$")) {
            return "+" + digitsAndPlus;
        }
        if (digitsAndPlus.matches("^0\\d{10}$")) {
            return "+91" + digitsAndPlus.substring(1);
        }
        return digitsAndPlus;
    }

    public static String normalizeLocale(String locale) {
        if (locale == null || locale.isBlank()) {
            return "en";
        }
        String lower = locale.trim().toLowerCase();
        if (lower.startsWith("te")) {
            return "te";
        }
        return "en";
    }
}
