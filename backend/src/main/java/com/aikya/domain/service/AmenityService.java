package com.aikya.domain.service;

import com.aikya.common.AikyaException;
import com.aikya.common.AuthenticatedUser;
import com.aikya.common.SecurityContextHelper;
import com.aikya.domain.dto.AmenityBookingDto;
import com.aikya.domain.dto.AmenityBookingRequest;
import com.aikya.domain.dto.AmenitySlotDto;
import com.aikya.domain.entity.Amenity;
import com.aikya.domain.entity.AmenityBooking;
import com.aikya.domain.entity.Unit;
import com.aikya.domain.repository.AmenityBookingRepository;
import com.aikya.domain.repository.AmenityRepository;
import com.aikya.domain.repository.UnitRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AmenityService {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final AmenityRepository amenityRepository;
    private final AmenityBookingRepository bookingRepository;
    private final UnitRepository unitRepository;

    public AmenityService(
            AmenityRepository amenityRepository,
            AmenityBookingRepository bookingRepository,
            UnitRepository unitRepository
    ) {
        this.amenityRepository = amenityRepository;
        this.bookingRepository = bookingRepository;
        this.unitRepository = unitRepository;
    }

    @Transactional(readOnly = true)
    public List<Amenity> listAmenities() {
        return amenityRepository.findByActiveTrueOrderByIdAsc();
    }

    @Transactional(readOnly = true)
    public List<AmenitySlotDto> getAvailableSlots(Long amenityId, LocalDate date) {
        Amenity amenity = amenityRepository.findById(amenityId)
                .orElseThrow(() -> new AikyaException(HttpStatus.NOT_FOUND, "Amenity not found: " + amenityId));
        LocalDate targetDate = date != null ? date : LocalDate.now();

        LocalTime open = parseTime(amenity.getOpenTime(), LocalTime.of(6, 0));
        LocalTime close = parseTime(amenity.getCloseTime(), LocalTime.of(22, 0));

        List<AmenityBooking> confirmedBookings = bookingRepository
                .findByAmenityIdAndBookingDateAndStatus(amenityId, targetDate, "CONFIRMED");

        List<AmenitySlotDto> slots = new ArrayList<>();
        LocalTime cursor = open;
        while (cursor.plusHours(2).compareTo(close) <= 0) {
            LocalTime slotEnd = cursor.plusHours(2);
            String conflictingUnit = null;
            for (AmenityBooking booking : confirmedBookings) {
                LocalTime bStart = parseTime(booking.getStartTime(), LocalTime.MIN);
                LocalTime bEnd = parseTime(booking.getEndTime(), LocalTime.MAX);
                if (bStart.isBefore(slotEnd) && bEnd.isAfter(cursor)) {
                    conflictingUnit = booking.getUnitNumber();
                    break;
                }
            }
            slots.add(new AmenitySlotDto(
                    cursor.format(TIME_FMT),
                    slotEnd.format(TIME_FMT),
                    conflictingUnit == null,
                    conflictingUnit
            ));
            cursor = slotEnd;
        }

        return slots;
    }

    @Transactional(readOnly = true)
    public List<AmenityBookingDto> listBookings() {
        List<AmenityBooking> bookings = bookingRepository.findAllByOrderByBookingDateDescStartTimeAsc();
        Map<Long, Amenity> amenityMap = amenityRepository.findAll().stream()
                .collect(Collectors.toMap(Amenity::getId, Function.identity()));
        return bookings.stream()
                .map(b -> toBookingDto(b, amenityMap.get(b.getAmenityId())))
                .toList();
    }

    @Transactional
    public AmenityBookingDto createBooking(AmenityBookingRequest request) {
        if (request == null || request.amenityId() == null) {
            throw new AikyaException(HttpStatus.BAD_REQUEST, "Amenity ID is required");
        }
        if (request.bookingDate() == null) {
            throw new AikyaException(HttpStatus.BAD_REQUEST, "Booking date is required");
        }
        if (request.startTime() == null || request.endTime() == null) {
            throw new AikyaException(HttpStatus.BAD_REQUEST, "Start time and end time are required");
        }

        Amenity amenity = amenityRepository.findById(request.amenityId())
                .orElseThrow(() -> new AikyaException(HttpStatus.NOT_FOUND, "Amenity not found: " + request.amenityId()));

        LocalTime reqStart = parseTime(request.startTime(), null);
        LocalTime reqEnd = parseTime(request.endTime(), null);
        if (reqStart == null || reqEnd == null || !reqStart.isBefore(reqEnd)) {
            throw new AikyaException(HttpStatus.BAD_REQUEST, "Start time must be before end time (HH:mm)");
        }

        List<AmenityBooking> existingConfirmed = bookingRepository
                .findByAmenityIdAndBookingDateAndStatus(amenity.getId(), request.bookingDate(), "CONFIRMED");

        for (AmenityBooking existing : existingConfirmed) {
            LocalTime existingStart = parseTime(existing.getStartTime(), LocalTime.MIN);
            LocalTime existingEnd = parseTime(existing.getEndTime(), LocalTime.MAX);
            if (existingStart.isBefore(reqEnd) && existingEnd.isAfter(reqStart)) {
                throw new AikyaException(HttpStatus.CONFLICT,
                        "Selected time slot conflicts with an existing booking for unit " + existing.getUnitNumber());
            }
        }

        AuthenticatedUser currentUser = SecurityContextHelper.getCurrentUser();
        Long unitId = request.unitId();
        String unitNumber = request.unitNumber() != null && !request.unitNumber().isBlank()
                ? request.unitNumber().trim()
                : currentUser.unitNumber();

        if (unitId == null && unitNumber != null) {
            unitId = unitRepository.findByUnitNumber(unitNumber)
                    .map(Unit::getId)
                    .orElse(null);
        }
        if (unitId == null) {
            Unit firstUnit = unitRepository.findAllByOrderByBlockNameAscUnitNumberAsc().stream()
                    .findFirst()
                    .orElseThrow(() -> new AikyaException(HttpStatus.BAD_REQUEST, "No unit found for booking"));
            unitId = firstUnit.getId();
            if (unitNumber == null) {
                unitNumber = firstUnit.getUnitNumber();
            }
        } else if (unitNumber == null) {
            unitNumber = unitRepository.findById(unitId)
                    .map(Unit::getUnitNumber)
                    .orElse("A-101");
        }

        double hours = Duration.between(reqStart, reqEnd).toMinutes() / 60.0;
        BigDecimal hourlyRate = amenity.getHourlyRate() != null ? amenity.getHourlyRate() : BigDecimal.ZERO;
        BigDecimal feeAmount = hourlyRate.multiply(BigDecimal.valueOf(hours)).setScale(2, RoundingMode.HALF_UP);

        AmenityBooking booking = new AmenityBooking();
        booking.setAmenityId(amenity.getId());
        booking.setUnitId(unitId);
        booking.setUnitNumber(unitNumber);
        booking.setBookedByUserId(currentUser.userId());
        booking.setBookedByName(currentUser.fullName());
        booking.setBookingDate(request.bookingDate());
        booking.setStartTime(reqStart.format(TIME_FMT));
        booking.setEndTime(reqEnd.format(TIME_FMT));
        booking.setPurpose(request.purpose() != null && !request.purpose().isBlank() ? request.purpose().trim() : "Resident Reservation");
        booking.setGuestCount(request.guestCount() != null && request.guestCount() > 0 ? request.guestCount() : 2);
        booking.setFeeAmount(feeAmount);
        booking.setStatus("CONFIRMED");
        booking.setCreatedAt(Instant.now());

        AmenityBooking saved = bookingRepository.save(booking);
        return toBookingDto(saved, amenity);
    }

    private LocalTime parseTime(String raw, LocalTime fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            String trimmed = raw.trim();
            if (trimmed.length() == 4 && trimmed.indexOf(':') == 1) {
                trimmed = "0" + trimmed;
            }
            return LocalTime.parse(trimmed.substring(0, 5), TIME_FMT);
        } catch (Exception e) {
            return fallback;
        }
    }

    public AmenityBookingDto toBookingDto(AmenityBooking b, Amenity amenity) {
        return new AmenityBookingDto(
                b.getId(),
                b.getAmenityId(),
                amenity != null ? amenity.getName() : "Amenity",
                amenity != null ? amenity.getNameTe() : "",
                b.getUnitId(),
                b.getUnitNumber(),
                b.getBookedByUserId(),
                b.getBookedByName(),
                b.getBookingDate(),
                b.getStartTime(),
                b.getEndTime(),
                b.getPurpose(),
                b.getGuestCount(),
                b.getFeeAmount(),
                b.getStatus(),
                b.getCreatedAt()
        );
    }
}
