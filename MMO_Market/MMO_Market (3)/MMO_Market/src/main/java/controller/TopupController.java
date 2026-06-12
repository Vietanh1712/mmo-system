package controller;

import controller.dto.SePayWebhookRequest;
import service.TopupService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sepay")
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class TopupController {

    @Autowired
    private TopupService topupService;

    @Value("${sepay.webhook.token:my_secret_sepay_token}")
    private String sepayWebhookToken;

    @Value("${sepay.bank.id:MB}")
    private String bankId;

    @Value("${sepay.bank.account-number:0987654321}")
    private String bankAccountNumber;

    @Value("${sepay.bank.account-name:NGUYEN THI NGOC LINH}")
    private String bankAccountName;

    @GetMapping("/config")
    public ResponseEntity<?> getSepayConfig() {
        log.info("Fetching public SePay bank details configuration");
        return ResponseEntity.ok(new Object() {
            public final String bankId = TopupController.this.bankId;
            public final String accountNumber = TopupController.this.bankAccountNumber;
            public final String accountName = TopupController.this.bankAccountName;
        });
    }

    @PostMapping("/webhook")
    public ResponseEntity<?> handleSepayWebhook(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody SePayWebhookRequest request) {

        log.info("Received SePay Webhook request. Authorization Header: {}", authHeader);

        // Verify authorization header (Expected: "Apikey <TOKEN>")
        String expectedAuth = "Apikey " + sepayWebhookToken;
        if (authHeader == null || !authHeader.equalsIgnoreCase(expectedAuth)) {
            log.warn("Unauthorized SePay Webhook access attempt. Auth Header: {}", authHeader);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new Object() {
                public final boolean success = false;
                public final String message = "Unauthorized";
            });
        }

        try {
            boolean processed = topupService.processSepayWebhook(request);
            if (processed) {
                return ResponseEntity.ok(new Object() {
                    public final boolean success = true;
                    public final String message = "Webhook processed successfully";
                });
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new Object() {
                    public final boolean success = false;
                    public final String message = "Failed to process webhook (Verify transfer content pattern or user ID)";
                });
            }
        } catch (Exception e) {
            log.error("Exception occurred while processing SePay Webhook: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new Object() {
                public final boolean success = false;
                public final String message = "Internal Server Error: " + e.getMessage();
            });
        }
    }
}
