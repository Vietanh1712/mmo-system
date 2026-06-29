package com.mmo.shared.dal;

import com.mmo.shared.model.KycRequest;
import com.mmo.shared.model.KycStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KycRequestRepository extends JpaRepository<KycRequest, Long> {
    Optional<KycRequest> findByRequestCode(String requestCode);
    Optional<KycRequest> findByIdAndIsDeleteFalse(Long id);
    List<KycRequest> findByUser_IdAndIsDeleteFalseOrderByCreatedAtDesc(Long userId);
    Optional<KycRequest> findByActiveUserId(Long activeUserId);
    Page<KycRequest> findAllByIsDeleteFalse(Pageable pageable);
    Page<KycRequest> findByStatusAndIsDeleteFalse(KycStatus status, Pageable pageable);
    boolean existsByRequestCode(String requestCode);
    boolean existsByActiveUserId(Long activeUserId);
    long countByStatusAndIsDeleteFalse(KycStatus status);
    long countByIsDeleteFalse();
}
