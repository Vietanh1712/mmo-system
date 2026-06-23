package service;

import dal.ProductRepository;
import dal.TransactionRepository;
import dal.UserRepository;
import model.Product;
import model.ProductVariant;
import model.Transaction;
import model.User;
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
    private dal.SystemConfigurationRepository systemConfigurationRepository;

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
                    catch (NumberFormatException e) { return 1000L; }
                }).orElse(1000L);

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
                .amountVnd(price)
                .commissionVnd(commission)
                .status("Held")
                .escrowReleaseDate(LocalDateTime.now().plusHours(escrowHoldHours))
                .build();

        return transactionRepository.save(transaction);
    }
}
