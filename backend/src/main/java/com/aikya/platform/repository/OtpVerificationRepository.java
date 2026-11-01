package com.aikya.platform.repository;

import com.aikya.platform.entity.OtpVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OtpVerificationRepository extends JpaRepository<OtpVerification, Long> {

    Optional<OtpVerification> findFirstByPhoneNumberAndVerifiedFalseOrderByCreatedAtDesc(String phoneNumber);

    List<OtpVerification> findByPhoneNumberOrderByCreatedAtDesc(String phoneNumber);
}
