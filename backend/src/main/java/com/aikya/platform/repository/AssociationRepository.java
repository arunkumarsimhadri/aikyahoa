package com.aikya.platform.repository;

import com.aikya.platform.entity.Association;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AssociationRepository extends JpaRepository<Association, Long> {

    Optional<Association> findByCode(String code);

    Optional<Association> findBySchemaName(String schemaName);

    List<Association> findByActiveTrueOrderByIdAsc();

    List<Association> findAllByOrderByIdAsc();

    boolean existsByCode(String code);

    boolean existsBySchemaName(String schemaName);
}
