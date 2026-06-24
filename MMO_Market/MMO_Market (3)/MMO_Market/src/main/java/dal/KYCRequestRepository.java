package dal;

import model.KYCRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface KYCRequestRepository
        extends JpaRepository<KYCRequest, Long> {


    long countByStatus(String status);


    // tìm theo tên
    Page<KYCRequest> findByFullNameContainingIgnoreCase(
            String fullName,
            Pageable pageable
    );


    // status
    Page<KYCRequest> findByStatus(
            String status,
            Pageable pageable
    );


    // loại giấy tờ
    Page<KYCRequest> findByTypeKyc(
            String typeKyc,
            Pageable pageable
    );


    // id
    Page<KYCRequest> findById(
            Long id,
            Pageable pageable
    );


    // tên + status
    Page<KYCRequest>
    findByFullNameContainingIgnoreCaseAndStatus(
            String name,
            String status,
            Pageable pageable
    );


    // tên + type
    Page<KYCRequest>
    findByFullNameContainingIgnoreCaseAndTypeKyc(
            String name,
            String typeKyc,
            Pageable pageable
    );


    // tên + status + type
    Page<KYCRequest>
    findByFullNameContainingIgnoreCaseAndStatusAndTypeKyc(
            String name,
            String status,
            String typeKyc,
            Pageable pageable
    );


    // id + status
    Page<KYCRequest>
    findByIdAndStatus(
            Long id,
            String status,
            Pageable pageable
    );


    // id + type
    Page<KYCRequest>
    findByIdAndTypeKyc(
            Long id,
            String typeKyc,
            Pageable pageable
    );


    // id + status + type
    Page<KYCRequest>
    findByIdAndStatusAndTypeKyc(
            Long id,
            String status,
            String typeKyc,
            Pageable pageable
    );

    Page<KYCRequest>
    findByStatusAndTypeKyc(
            String status,
            String typeKyc,
            Pageable pageable
    );
}