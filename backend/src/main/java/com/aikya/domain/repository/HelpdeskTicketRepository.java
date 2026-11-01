package com.aikya.domain.repository;

import com.aikya.domain.entity.HelpdeskTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HelpdeskTicketRepository extends JpaRepository<HelpdeskTicket, Long> {

    List<HelpdeskTicket> findAllByOrderByCreatedAtDesc();

    List<HelpdeskTicket> findByStatusOrderByCreatedAtDesc(String status);

    List<HelpdeskTicket> findByUnitIdOrderByCreatedAtDesc(Long unitId);
}
