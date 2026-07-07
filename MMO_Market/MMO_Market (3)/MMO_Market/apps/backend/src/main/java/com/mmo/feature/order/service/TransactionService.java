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

    /**
     * Thực hiện mua sản phẩm và trừ tiền từ số dư của người mua, bọc trong Transaction.
     */
    @Transactional
    public Transaction purchaseProduct(Long userId, Long productId, String variantLabel, Integer quantity) {
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

        // Đọc cấu hình thời gian giam tiền Escrow (Escrow Hold Hours)
        int escrowHoldHours = systemConfigurationRepository.findByConfigKey("ESCROW_HOLD_HOURS")
                .map(c -> {
                    try { return Integer.parseInt(c.getConfigValue()); }
                    catch (NumberFormatException e) { return 72; }
                }).orElse(72);

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
