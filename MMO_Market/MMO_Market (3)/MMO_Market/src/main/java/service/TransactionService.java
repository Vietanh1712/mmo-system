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

        // Kiểm tra số dư ví người mua
        long price = variant.getPriceVnd();
        if (customer.getBalanceVnd() == null || customer.getBalanceVnd() < price) {
            throw new IllegalArgumentException("Số dư tài khoản không đủ để thực hiện thanh toán.");
        }

        // Trừ tiền người mua
        customer.setBalanceVnd(customer.getBalanceVnd() - price);
        userRepository.save(customer);

        // Giảm tồn kho
        variant.setStock(variant.getStock() - 1);

        // Tính phí hoa hồng (5%)
        long commission = (long) (price * 0.05);

        // Tạo giao dịch mới (trạng thái Held để bảo lãnh Escrow 3 ngày)
        Transaction transaction = Transaction.builder()
                .customer(customer)
                .seller(product.getSeller())
                .product(product)
                .variant(variant)
                .amountVnd(price)
                .commissionVnd(commission)
                .status("Held")
                .escrowReleaseDate(LocalDateTime.now().plusDays(3))
                .build();

        return transactionRepository.save(transaction);
    }
}
