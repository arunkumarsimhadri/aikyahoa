package com.aikya.domain.repository;

import com.aikya.domain.entity.Unit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UnitRepository extends JpaRepository<Unit, Long> {

    Optional<Unit> findByUnitNumber(String unitNumber);

    List<Unit> findAllByOrderByBlockNameAscUnitNumberAsc();
}
