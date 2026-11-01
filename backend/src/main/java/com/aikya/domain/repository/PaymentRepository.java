package com.aikya.domain.repository;

import com.aikya.domain.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByReconciliationStatusOrderByCreatedAtDesc(String reconciliationStatus);

    List<Payment> findByInvoiceIdOrderByCreatedAtDesc(Long invoiceId);

    List<Payment> findAllByOrderByCreatedAtDesc();
}
