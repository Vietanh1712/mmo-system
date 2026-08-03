package com.mmo.shared.dal;

import com.mmo.shared.model.SellerBankInfo;
import com.mmo.shared.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository quản lý thông tin tài khoản ngân hàng của Người bán (SellerBankInfo).
 * Cung cấp các thao tác truy xuất thông tin ngân hàng phục vụ quy trình rút tiền.
 */
@Repository
public interface SellerBankInfoRepository extends JpaRepository<SellerBankInfo, Long> {
    
    /**
     * Tìm thông tin tài khoản ngân hàng chưa bị xóa của một người bán.
     */
    Optional<SellerBankInfo> findByUserAndIsDeleteFalse(User user);
    
    /**
     * Tìm thông tin tài khoản ngân hàng mới nhất chưa bị xóa của một người bán.
     */
    Optional<SellerBankInfo> findFirstByUserAndIsDeleteFalseOrderByIdDesc(User user);
    
    /**
     * Lấy toàn bộ danh sách thông tin tài khoản ngân hàng đã từng khai báo của một người bán.
     */
    List<SellerBankInfo> findByUser(User user);
}
