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
    public Transaction purchaseProduct(Long userId, Long productId, String variantLabel, Integer quantity) {
        User customer = userRepository.findByIdAndIsDeleteFalse(userId)
                .orElseThrow(() -> new IllegalArgumentException("Người mua không tồn tại."));

        Product product = productRepository.findByIdAndIsDeleteFalse(productId)
                .orElseThrow(() -> new IllegalArgumentException("Sản phẩm không tồn tại."));

        User seller = product.getSeller();
        if (seller == null || Boolean.TRUE.equals(seller.getIsDelete())) {
            throw new IllegalArgumentException("Người bán không tồn tại hoặc đã bị xóa.");
        }

        if (seller.getShopStatus() != null &&
                ("Suspended".equalsIgnoreCase(seller.getShopStatus()) || "TEMP_LOCKED".equalsIgnoreCase(seller.getShopStatus())) &&
                seller.getSuspendedUntil() != null &&
                LocalDateTime.now().isAfter(seller.getSuspendedUntil())) {
            seller.setShopStatus("Active");
            seller.setSuspendedUntil(null);
            userRepository.save(seller);
        }

        String sellerShopStatus = seller.getShopStatus();
        if (sellerShopStatus != null) {
            if ("Suspended".equalsIgnoreCase(sellerShopStatus) || "TEMP_LOCKED".equalsIgnoreCase(sellerShopStatus)) {
                throw new IllegalArgumentException("Cửa hàng của người bán đang tạm ngưng hoạt động, không thể mua sản phẩm.");
            }
            if ("Locked".equalsIgnoreCase(sellerShopStatus) || "INDEFINITE_LOCKED".equalsIgnoreCase(sellerShopStatus)) {
                throw new IllegalArgumentException("Cửa hàng của người bán đang bị tạm khóa, không thể mua sản phẩm.");
            }
            if ("Banned".equalsIgnoreCase(sellerShopStatus) || "PERMANENT_BANNED".equalsIgnoreCase(sellerShopStatus)) {
                throw new IllegalArgumentException("Cửa hàng của người bán đã bị khóa vĩnh viễn, không thể mua sản phẩm.");
            }
            if ("Withdrawn".equalsIgnoreCase(sellerShopStatus) || "DELETED".equalsIgnoreCase(sellerShopStatus) || "Pending".equalsIgnoreCase(sellerShopStatus)) {
                throw new IllegalArgumentException("Cửa hàng của người bán hiện không ở trạng thái hoạt động, không thể mua sản phẩm.");
            }
        }

        // Tìm variant phù hợp theo variantLabel (variantName) với cơ chế tìm kiếm mềm và fallback
        ProductVariant variant = product.getVariants().stream()
                .filter(v -> v.getIsDelete() != null && !v.getIsDelete())
                .filter(v -> v.getVariantName().equalsIgnoreCase(variantLabel))
                .findFirst()
                .orElseGet(() -> {
                    // Thử tìm kiếm theo cụm từ chứa (substring)
                    return product.getVariants().stream()
                            .filter(v -> v.getIsDelete() != null && !v.getIsDelete())
                            .filter(v -> v.getVariantName().toLowerCase().contains(variantLabel.toLowerCase())
                                    || variantLabel.toLowerCase().contains(v.getVariantName().toLowerCase()))
                            .findFirst()
                            // Fallback về biến thể đầu tiên khả dụng của sản phẩm
                            .orElseGet(() -> product.getVariants().stream()
                                    .filter(v -> v.getIsDelete() != null && !v.getIsDelete())
                                    .findFirst()
                                    .orElseThrow(() -> new IllegalArgumentException("Biến thể sản phẩm không tồn tại và sản phẩm không có biến thể khả dụng.")));
                });

        if (quantity == null || quantity <= 0) {
            quantity = 1;
        }

        // Kiểm tra số lượng tồn kho (stock)
        if (variant.getStock() == null || variant.getStock() < quantity) {
            throw new IllegalArgumentException("Sản phẩm không đủ số lượng tồn kho.");
        }

        // Đọc cấu hình phí cố định người mua (Flat Buyer Fee)
        long flatBuyerFee = systemConfigurationRepository.findByConfigKey("FLAT_BUYER_FEE_VND")
                .map(c -> {
                    try { return Long.parseLong(c.getConfigValue()); }
                    catch (NumberFormatException e) { return 0L; }
                }).orElse(0L);

        // Kiểm tra số dư ví người mua (bao gồm cả phí cố định người mua)
        long price = variant.getPriceVnd();
        long totalAmount = price * quantity;
        long totalDebit = totalAmount + flatBuyerFee;
        if (customer.getBalanceVnd() == null || customer.getBalanceVnd() < totalDebit) {
            throw new IllegalArgumentException("Số dư tài khoản không đủ để thực hiện thanh toán (cần " + totalDebit + " VNĐ).");
        }

        // Trừ tiền người mua
        customer.setBalanceVnd(customer.getBalanceVnd() - totalDebit);
        userRepository.save(customer);

        // Giảm tồn kho
        variant.setStock(variant.getStock() - quantity);

        // Đọc cấu hình hoa hồng mặc định của sàn (Commission Percent)
        double basePercent = systemConfigurationRepository.findByConfigKey("DEFAULT_COMMISSION_PERCENT")
                .map(c -> {
                    try { return Double.parseDouble(c.getConfigValue()); }
                    catch (NumberFormatException e) { return 5.0; }
                }).orElse(5.0);

        // Tính phí hoa hồng động
        long commission = (long) (totalAmount * (basePercent / 100.0));

        // Tính toán thời gian giam tiền Escrow (Escrow Hold Hours) động
        int escrowHoldHours = 72; // Mặc định 3 ngày
        
<<<<<<< HEAD
        long completedCount = transactionRepository.countCompletedSalesBySeller(seller);
        long totalSold = transactionRepository.countTotalSalesBySeller(seller);
        long resolvedComplaints = complaintRepository.countResolvedComplaintsBySeller(seller);
=======
        User seller = product.getSeller();
        int shopLevel = seller.getShopLevel() != null ? seller.getShopLevel() : 1;
>>>>>>> 87e66c1274e32dbd86986e7404238481baf26ec2
        
        if (shopLevel == 1) {
            // Giai đoạn thử thách (Shop mới Level 1)
            escrowHoldHours = 168; // 7 ngày
        } else if (shopLevel == 0) {
            // Giai đoạn thắt chặt (Shop Cảnh cáo Level 0)
            escrowHoldHours = 168; // 7 ngày
        } else {
            // Giai đoạn tiêu chuẩn (Shop Uy tín Level 2)
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
                .amountVnd(totalAmount)
                .commissionVnd(commission)
                .quantity(quantity)
                .status("Held")
                .escrowReleaseDate(LocalDateTime.now().plusHours(escrowHoldHours))
                .build();
        transaction = transactionRepository.save(transaction);

        // Gán DigitalAsset nếu có
        java.util.List<com.mmo.shared.model.DigitalAsset> availableAssets = digitalAssetRepository.findByVariantAndIsUsedFalseAndIsDeleteFalse(variant);
        if (!availableAssets.isEmpty()) {
            if (availableAssets.size() < quantity) {
                throw new IllegalArgumentException("Không đủ tài khoản/key trong kho để giao. Vui lòng giảm số lượng mua.");
            }
            for (int i = 0; i < quantity; i++) {
                com.mmo.shared.model.DigitalAsset assetToAssign = availableAssets.get(i);
                assetToAssign.setIsUsed(true);
                assetToAssign.setTransaction(transaction);
                digitalAssetRepository.save(assetToAssign);
            }
        }

        // Record to Wallet Ledger (Customer paid)
        walletService.recordTransaction(customer, "PAYMENT", -totalAmount, "SUCCESS", "Thanh toán đơn hàng " + product.getName(), "MMO-ORD-" + transaction.getId(), customer.getBalanceVnd());

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
