package com.aikya.platform.service;

import com.aikya.common.AikyaException;
import com.aikya.common.AssociationConfigLookupPort;
import com.aikya.common.AuthenticatedUser;
import com.aikya.common.BillingType;
import com.aikya.common.Role;
import com.aikya.common.SecurityContextHelper;
import com.aikya.common.TenantContext;
import com.aikya.platform.dto.AssociationCreateRequest;
import com.aikya.platform.dto.AssociationDto;
import com.aikya.platform.entity.Association;
import com.aikya.platform.entity.AssociationMembership;
import com.aikya.platform.multitenancy.TenantMigrationService;
import com.aikya.platform.repository.AssociationMembershipRepository;
import com.aikya.platform.repository.AssociationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class AssociationService implements AssociationConfigLookupPort {

    private final AssociationRepository associationRepository;
    private final AssociationMembershipRepository membershipRepository;
    private final TenantMigrationService tenantMigrationService;

    public AssociationService(
            AssociationRepository associationRepository,
            AssociationMembershipRepository membershipRepository,
            TenantMigrationService tenantMigrationService
    ) {
        this.associationRepository = associationRepository;
        this.membershipRepository = membershipRepository;
        this.tenantMigrationService = tenantMigrationService;
    }

    @Override
    @Transactional(readOnly = true)
    public AssociationBillingConfig getCurrentAssociationConfig(String schemaName) {
        String targetSchema = (schemaName != null && !schemaName.isBlank())
                ? schemaName.trim().toLowerCase()
                : TenantContext.getCurrentTenant();

        Association association = associationRepository.findBySchemaName(targetSchema)
                .orElseGet(() -> associationRepository.findByActiveTrueOrderByIdAsc()
                        .stream()
                        .findFirst()
                        .orElseThrow(() -> new AikyaException(
                                HttpStatus.NOT_FOUND,
                                "Association configuration not found for schema: " + targetSchema
                        )));

        return new AssociationBillingConfig(
                association.getId(),
                association.getName(),
                association.getCode(),
                association.getSchemaName(),
                association.getBillingType(),
                association.getRatePerSqft(),
                association.getFlatMaintenanceAmount(),
                association.getSinkingFundAmount(),
                association.getWaterChargePerUnit(),
                association.getLateFeePercentage()
        );
    }

    @Transactional(readOnly = true)
    public List<AssociationDto> listActiveAssociations() {
        return associationRepository.findByActiveTrueOrderByIdAsc()
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public AssociationDto getAssociationById(Long id) {
        Association association = associationRepository.findById(id)
                .orElseThrow(() -> new AikyaException(HttpStatus.NOT_FOUND, "Association not found: " + id));
        return toDto(association);
    }

    @Transactional
    public AssociationDto createAssociation(AssociationCreateRequest request) {
        String code = resolveUniqueCode(request.code(), request.name());

        Association association = new Association();
        association.setName(request.name().trim());
        association.setNameTe(request.nameTe() != null && !request.nameTe().isBlank() ? request.nameTe().trim() : request.name().trim());
        association.setCode(code);
        association.setSchemaName("aikya_assoc_tmp_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
        association.setAddress(request.address().trim());
        association.setCity(request.city() != null && !request.city().isBlank() ? request.city().trim() : "Hyderabad");
        association.setState(request.state() != null && !request.state().isBlank() ? request.state().trim() : "Telangana");
        association.setPincode(request.pincode() != null && !request.pincode().isBlank() ? request.pincode().trim() : "500081");
        association.setBillingType(request.billingType() != null ? request.billingType() : BillingType.AREA_BASED);
        association.setRatePerSqft(request.ratePerSqft() != null ? request.ratePerSqft() : new BigDecimal("3.50"));
        association.setFlatMaintenanceAmount(request.flatMaintenanceAmount() != null ? request.flatMaintenanceAmount() : new BigDecimal("3500.00"));
        association.setSinkingFundAmount(request.sinkingFundAmount() != null ? request.sinkingFundAmount() : new BigDecimal("500.00"));
        association.setWaterChargePerUnit(request.waterChargePerUnit() != null ? request.waterChargePerUnit() : new BigDecimal("450.00"));
        association.setLateFeePercentage(request.lateFeePercentage() != null ? request.lateFeePercentage() : new BigDecimal("2.00"));
        association.setActive(true);

        Association saved = associationRepository.saveAndFlush(association);
        String finalSchemaName = "aikya_assoc_" + saved.getId();
        saved.setSchemaName(finalSchemaName);
        saved = associationRepository.saveAndFlush(saved);

        tenantMigrationService.provisionAndMigrateTenantSchema(finalSchemaName);

        AuthenticatedUser currentUser = SecurityContextHelper.getCurrentUserOrNull();
        if (currentUser != null && currentUser.userId() != null) {
            List<AssociationMembership> existing = membershipRepository.findByUserIdAndAssociationId(
                    currentUser.userId(), saved.getId()
            );
            if (existing.isEmpty()) {
                AssociationMembership membership = new AssociationMembership();
                membership.setUserId(currentUser.userId());
                membership.setAssociationId(saved.getId());
                membership.setRole(Role.PRESIDENT);
                membership.setUnitNumber("A-101");
                membership.setBlockName("Block A");
                membership.setActive(true);
                membershipRepository.save(membership);
            }
        }

        return toDto(saved);
    }

    private String resolveUniqueCode(String requestedCode, String name) {
        String base = (requestedCode != null && !requestedCode.isBlank())
                ? requestedCode.trim()
                : name.trim();
        String sanitized = base.toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        if (sanitized.isBlank()) {
            sanitized = "ASSOC";
        }
        if (sanitized.length() > 48) {
            sanitized = sanitized.substring(0, 48);
        }
        String candidate = sanitized;
        int suffix = 1;
        while (associationRepository.existsByCode(candidate)) {
            candidate = sanitized + "_" + suffix++;
        }
        return candidate;
    }

    public AssociationDto toDto(Association association) {
        return new AssociationDto(
                association.getId(),
                association.getName(),
                association.getNameTe(),
                association.getCode(),
                association.getSchemaName(),
                association.getAddress(),
                association.getCity(),
                association.getState(),
                association.getPincode(),
                association.getBillingType(),
                association.getRatePerSqft(),
                association.getFlatMaintenanceAmount(),
                association.getSinkingFundAmount(),
                association.getWaterChargePerUnit(),
                association.getLateFeePercentage(),
                association.isActive(),
                association.getCreatedAt()
        );
    }
}
