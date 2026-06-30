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

    @org.springframework.data.jpa.repository.Query("SELECT k FROM KycRequest k WHERE k.isDelete = false " +
            "AND (:status IS NULL OR k.status = :status) " +
            "AND (:requestCode IS NULL OR LOWER(k.requestCode) LIKE LOWER(CONCAT('%', :requestCode, '%'))) " +
            "AND (:idType IS NULL OR k.idType = :idType)")
    Page<KycRequest> searchKycRequests(
            @org.springframework.data.repository.query.Param("status") com.mmo.shared.model.KycStatus status,
            @org.springframework.data.repository.query.Param("requestCode") String requestCode,
            @org.springframework.data.repository.query.Param("idType") com.mmo.shared.model.IdType idType,
            Pageable pageable);

    boolean existsByRequestCode(String requestCode);
    boolean existsByActiveUserId(Long activeUserId);
    long countByStatusAndIsDeleteFalse(KycStatus status);
    long countByIsDeleteFalse();
}
