package com.aikya.domain.repository;

import com.aikya.domain.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    boolean existsByUnitIdAndBillingMonthAndBillingYear(Long unitId, Integer billingMonth, Integer billingYear);

    List<Invoice> findByUnitIdOrderByBillingYearDescBillingMonthDesc(Long unitId);

    List<Invoice> findByStatusOrderByDueDateAsc(String status);

    List<Invoice> findAllByOrderByCreatedAtDesc();
}
