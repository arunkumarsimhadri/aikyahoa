package com.aikya.platform.controller;

import com.aikya.common.ApiResponse;
import com.aikya.platform.dto.AssociationCreateRequest;
import com.aikya.platform.dto.AssociationDto;
import com.aikya.platform.service.AssociationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/associations")
public class AssociationController {

    private final AssociationService associationService;

    public AssociationController(AssociationService associationService) {
        this.associationService = associationService;
    }

    @GetMapping
    public ApiResponse<List<AssociationDto>> listAssociations() {
        return ApiResponse.ok(associationService.listActiveAssociations());
    }

    @GetMapping("/{id}")
    public ApiResponse<AssociationDto> getAssociation(@PathVariable Long id) {
        return ApiResponse.ok(associationService.getAssociationById(id));
    }

    @PostMapping
    public ApiResponse<AssociationDto> createAssociation(@Valid @RequestBody AssociationCreateRequest request) {
        AssociationDto created = associationService.createAssociation(request);
        return ApiResponse.ok("Association created and tenant schema provisioned", created);
    }
}
