package com.aikya.domain.repository;

import com.aikya.domain.entity.Amenity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AmenityRepository extends JpaRepository<Amenity, Long> {

    List<Amenity> findByActiveTrueOrderByIdAsc();

    Optional<Amenity> findByCode(String code);
}
