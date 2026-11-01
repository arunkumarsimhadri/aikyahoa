package com.aikya.platform.repository;

import com.aikya.platform.entity.AssociationMembership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AssociationMembershipRepository extends JpaRepository<AssociationMembership, Long> {

    List<AssociationMembership> findByUserIdAndActiveTrueOrderByIdAsc(Long userId);

    List<AssociationMembership> findByUserIdAndActiveTrue(Long userId);

    List<AssociationMembership> findByUserId(Long userId);

    List<AssociationMembership> findByAssociationIdAndActiveTrue(Long associationId);

    List<AssociationMembership> findByAssociationId(Long associationId);

    List<AssociationMembership> findByUserIdAndAssociationId(Long userId, Long associationId);

    Optional<AssociationMembership> findByUserIdAndAssociationIdAndUnitNumber(
            Long userId, Long associationId, String unitNumber
    );
}
