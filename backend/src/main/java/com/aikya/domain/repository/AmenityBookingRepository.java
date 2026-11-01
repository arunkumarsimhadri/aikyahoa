package com.aikya.domain.repository;

import com.aikya.domain.entity.AmenityBooking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface AmenityBookingRepository extends JpaRepository<AmenityBooking, Long> {

    List<AmenityBooking> findByAmenityIdAndBookingDateAndStatus(Long amenityId, LocalDate bookingDate, String status);

    List<AmenityBooking> findAllByOrderByBookingDateDescStartTimeAsc();

    List<AmenityBooking> findByBookingDateGreaterThanEqualAndStatusOrderByBookingDateAscStartTimeAsc(LocalDate date, String status);
}
