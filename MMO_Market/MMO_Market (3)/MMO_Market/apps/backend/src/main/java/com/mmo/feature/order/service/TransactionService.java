package com.mmo.feature.order.service;
import com.mmo.shared.dal.DigitalAssetRepository;
import com.mmo.feature.wallet.service.WalletService;
import com.mmo.shared.dal.SystemConfigurationRepository;
import com.mmo.shared.model.DigitalAsset;

import com.mmo.shared.dal.ProductRepository;
import com.mmo.shared.dal.TransactionRepository;
import com.mmo.shared.dal.UserRepository;
import com.mmo.shared.model.Product;
import com.mmo.shared.model.ProductVariant;
import com.mmo.shared.model.Transaction;
import com.mmo.shared.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class TransactionService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private WalletService walletService;

    @Autowired
    private com.mmo.shared.dal.DigitalAssetRepository digitalAssetRepository;

    @Autowired
    private com.mmo.shared.dal.SystemConfigurationRepository systemConfigurationRepository;

    @Autowired
    private com.mmo.shared.dal.ComplaintRepository complaintRepository;

    /**
     * Thực hiện mua sản phẩm và trừ tiền từ số dư của người mua, bọc trong Transaction.
     */
    @Transactional
    public Transaction purchaseProduct(Long userId, Long productId, String variantLabel) {
        User customer = userRepository.findByIdAndIsDeleteFalse(userId)
                .orElseThrow(() -> new IllegalArgumentException("Người mua không tồn tại."));

        Product product = productRepository.findByIdAndIsDeleteFalse(productId)
                .orElseThrow(() -> new IllegalArgumentException("Sản phẩm không tồn tại."));

        // Tìm variant phù hợp theo variantLabel (variantName)
        ProductVariant variant = product.getVariants().stream()
                .filter(v -> v.getIsDelete() != null && !v.getIsDelete())
                .filter(v -> v.getVariantName().equalsIgnoreCase(variantLabel))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Biến thể sản phẩm không tồn tại."));

        // Kiểm tra số lượng tồn kho (stock)
        if (variant.getStock() == null || variant.getStock() <= 0) {
            throw new IllegalArgumentException("Sản phẩm đã hết hàng.");
        }

        // Đọc cấu hình phí cố định người mua (Flat Buyer Fee)
        long flatBuyerFee = systemConfigurationRepository.findByConfigKey("FLAT_BUYER_FEE_VND")
                .map(c -> {
                    try { return Long.parseLong(c.getConfigValue()); }
                    catch (NumberFormatException e) { return 0L; }
                }).orElse(0L);

        // Kiểm tra số dư ví người mua (bao gồm cả phí cố định người mua)
        long price = variant.getPriceVnd();
        long totalDebit = price + flatBuyerFee;
        if (customer.getBalanceVnd() == null || customer.getBalanceVnd() < totalDebit) {
            throw new IllegalArgumentException("Số dư tài khoản không đủ để thực hiện thanh toán (bao gồm phí người mua: " + flatBuyerFee + " VNĐ).");
        }

        // Trừ tiền người mua
        customer.setBalanceVnd(customer.getBalanceVnd() - totalDebit);
        userRepository.save(customer);

        // Giảm tồn kho
        variant.setStock(variant.getStock() - 1);

        // Đọc cấu hình hoa hồng mặc định của sàn (Commission Percent)
        double basePercent = systemConfigurationRepository.findByConfigKey("DEFAULT_COMMISSION_PERCENT")
                .map(c -> {
                    try { return Double.parseDouble(c.getConfigValue()); }
                    catch (NumberFormatException e) { return 5.0; }
                }).orElse(5.0);

        // Tính phí hoa hồng động
        long commission = (long) (price * (basePercent / 100.0));

        // Tính toán thời gian giam tiền Escrow (Escrow Hold Hours) động
        int escrowHoldHours = 72; // Mặc định 3 ngày
        
        User seller = product.getSeller();
        long completedCount = transactionRepository.countCompletedSalesBySeller(seller);
        long totalSold = transactionRepository.countTotalSalesBySeller(seller);
        long resolvedComplaints = complaintRepository.countResolvedComplaintsBySeller(seller);
        
        double disputeRate = totalSold > 0 ? (double) resolvedComplaints / totalSold : 0.0;
        
        if (completedCount < 20) {
            // Giai đoạn thử thách (20 đơn đầu tiên của Shop mới)
            escrowHoldHours = 168; // 7 ngày
        } else if (disputeRate >= 0.02) {
            // Giai đoạn thắt chặt (Tỷ lệ khiếu nại đúng >= 2%)
            escrowHoldHours = 168; // 7 ngày
        } else {
            // Giai đoạn tiêu chuẩn
            escrowHoldHours = systemConfigurationRepository.findByConfigKey("ESCROW_HOLD_HOURS")
                    .map(c -> {
                        try { return Integer.parseInt(c.getConfigValue()); }
                        catch (NumberFormatException e) { return 72; }
                    }).orElse(72);
        }

        // Tạo giao dịch mới (trạng thái Held để bảo lãnh Escrow)
        Transaction transaction = Transaction.builder()
                .customer(customer)
                .seller(product.getSeller())
                .product(product)
                .variant(variant)
                .amountVnd(price)
                .commissionVnd(commission)
                .status("Held")
                .escrowReleaseDate(LocalDateTime.now().plusHours(escrowHoldHours))
                .build();
        transaction = transactionRepository.save(transaction);

        // Gán DigitalAsset nếu có
        java.util.List<com.mmo.shared.model.DigitalAsset> availableAssets = digitalAssetRepository.findByVariantAndIsUsedFalseAndIsDeleteFalse(variant);
        if (!availableAssets.isEmpty()) {
            com.mmo.shared.model.DigitalAsset assetToAssign = availableAssets.get(0);
            assetToAssign.setIsUsed(true);
            assetToAssign.setTransaction(transaction);
            digitalAssetRepository.save(assetToAssign);
        }

        // Record to Wallet Ledger (Customer paid)
        walletService.recordTransaction(customer, "PAYMENT", -price, "SUCCESS", "Thanh toán đơn hàng " + product.getName(), "MMO-ORD-" + transaction.getId(), customer.getBalanceVnd());

        return transaction;
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public java.util.List<Transaction> getMyOrders(Long userId) {
        User customer = userRepository.findByIdAndIsDeleteFalse(userId)
                .orElseThrow(() -> new IllegalArgumentException("Người mua không tồn tại."));
        return transactionRepository.findByCustomerAndIsDeleteFalseOrderByCreatedAtDesc(customer);
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public Transaction getTransactionDetail(Long transactionId, Long userId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng."));
        
        if (!transaction.getCustomer().getId().equals(userId)) {
            throw new IllegalArgumentException("Bạn không có quyền xem đơn hàng này.");
        }
        
        return transaction;
    }
}
