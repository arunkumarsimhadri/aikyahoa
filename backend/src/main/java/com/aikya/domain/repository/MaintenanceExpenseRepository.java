package com.aikya.domain.repository;

import com.aikya.domain.entity.MaintenanceExpense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MaintenanceExpenseRepository extends JpaRepository<MaintenanceExpense, Long> {

    List<MaintenanceExpense> findAllByOrderByExpenseDateDescCreatedAtDesc();

    List<MaintenanceExpense> findByCategoryOrderByExpenseDateDescCreatedAtDesc(String category);
}
