package com.aikya.domain.controller;

import com.aikya.common.ApiResponse;
import com.aikya.domain.dto.AmenityBookingDto;
import com.aikya.domain.dto.AmenityBookingRequest;
import com.aikya.domain.dto.AmenitySlotDto;
import com.aikya.domain.entity.Amenity;
import com.aikya.domain.service.AmenityService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/amenities")
public class AmenityController {

    private final AmenityService amenityService;

    public AmenityController(AmenityService amenityService) {
        this.amenityService = amenityService;
    }

    @GetMapping
    public ApiResponse<List<Amenity>> listAmenities() {
        return ApiResponse.ok(amenityService.listAmenities());
    }

    @GetMapping("/{amenityId}/slots")
    public ApiResponse<List<AmenitySlotDto>> getAvailableSlots(
            @PathVariable Long amenityId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ApiResponse.ok(amenityService.getAvailableSlots(amenityId, date));
    }

    @GetMapping("/bookings")
    public ApiResponse<List<AmenityBookingDto>> listBookings() {
        return ApiResponse.ok(amenityService.listBookings());
    }

    @PostMapping("/bookings")
    public ApiResponse<AmenityBookingDto> createBooking(@RequestBody AmenityBookingRequest request) {
        return ApiResponse.ok("Amenity slot reserved", amenityService.createBooking(request));
    }
}
