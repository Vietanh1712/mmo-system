package com.mmo.shared.dal;

import com.mmo.shared.model.SellerBankInfo;
import com.mmo.shared.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SellerBankInfoRepository extends JpaRepository<SellerBankInfo, Long> {
    Optional<SellerBankInfo> findByUserAndIsDeleteFalse(User user);
    List<SellerBankInfo> findByUser(User user);
}
