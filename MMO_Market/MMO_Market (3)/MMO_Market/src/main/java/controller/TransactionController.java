package controller;

import controller.dto.PurchaseRequest;
import controller.dto.PurchaseResponse;
import model.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import service.TransactionService;

import java.util.Map;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    @Autowired
    private TransactionService transactionService;

    @PostMapping("/purchase")
    public ResponseEntity<?> purchaseProduct(
            @AuthenticationPrincipal Long userId,
            @RequestBody PurchaseRequest request) {

        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Vui lòng đăng nhập trước khi thực hiện mua hàng."));
        }

        if (request.getProductId() == null || request.getVariantLabel() == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Dữ liệu yêu cầu không hợp lệ."));
        }

        try {
            // Thực hiện transaction bọc trong service layer
            Transaction transaction = transactionService.purchaseProduct(userId, request.getProductId(), request.getVariantLabel());

            // Tự sinh credentials (mock) cho tài khoản số nếu sản phẩm thuộc dạng tài khoản số/key bản quyền
            String productName = transaction.getProduct().getName();
            String lowerName = productName.toLowerCase();
            boolean isAccount = lowerName.contains("tài khoản") ||
                    lowerName.contains("premium") ||
                    lowerName.contains("spotify") ||
                    lowerName.contains("netflix") ||
                    lowerName.contains("canva") ||
                    lowerName.contains("chatgpt") ||
                    lowerName.contains("gmail") ||
                    lowerName.contains("vpn") ||
                    lowerName.contains("key");

            PurchaseResponse.CredentialsDTO credentialsDTO = null;
            if (isAccount) {
                int randomNum = (int) (1000 + Math.random() * 9000);
                String cleanName = productName.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
                if (cleanName.length() > 8) {
                    cleanName = cleanName.substring(0, 8);
                }

                if (lowerName.contains("key")) {
                    credentialsDTO = PurchaseResponse.CredentialsDTO.builder()
                            .username("KEY-" + randomNum + "-ABCD-EFGH-IJKL")
                            .password("(Product Key)")
                            .build();
                } else {
                    credentialsDTO = PurchaseResponse.CredentialsDTO.builder()
                            .username(cleanName + "_" + randomNum + "@gmail.com")
                            .password("Pass_" + randomNum + "_Secure")
                            .build();
                }
            }

            // Sinh mã giao dịch và mã đơn hàng (mock format)
            String transactionCode = "TXN" + (10000000 + (long) (Math.random() * 90000000));
            String orderCode = "MMO-ORD-" + (1000 + (int) (Math.random() * 9000));

            PurchaseResponse response = PurchaseResponse.builder()
                    .transactionCode(transactionCode)
                    .orderCode(orderCode)
                    .finalBalance(transaction.getCustomer().getBalanceVnd())
                    .productName(productName)
                    .amount(transaction.getAmountVnd())
                    .credentials(credentialsDTO)
                    .build();

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "Lỗi hệ thống khi thực hiện thanh toán: " + e.getMessage()));
        }
    }
}
