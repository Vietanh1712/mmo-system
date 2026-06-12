package service;

import controller.dto.SePayWebhookRequest;
import dal.TopupTransactionRepository;
import dal.UserRepository;
import model.TopupTransaction;
import model.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class TopupService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TopupTransactionRepository topupTransactionRepository;

    private static final Pattern TRANSFER_CONTENT_PATTERN = Pattern.compile("MMO[\\s-]*TOPUP[\\s-]*(\\d+)", Pattern.CASE_INSENSITIVE);

    @Transactional
    public boolean processSepayWebhook(SePayWebhookRequest request) {
        log.info("Processing SePay Webhook transaction: {}, content: '{}', amount: {}", 
                request.getId(), request.getContent(), request.getTransferAmount());

        // 1. Validate transfer type is 'in' (deposit)
        if (!"in".equalsIgnoreCase(request.getTransferType())) {
            log.warn("Ignored non-deposit transaction (transferType: {})", request.getTransferType());
            return false;
        }

        // 2. Check if transaction has already been processed
        String sepayCode = String.valueOf(request.getId());
        if (topupTransactionRepository.findBySepayCode(sepayCode).isPresent()) {
            log.warn("SePay transaction {} already processed.", sepayCode);
            return true; // Return true as it is already complete
        }

        // 3. Parse user ID from transfer content
        String content = request.getContent();
        if (content == null || content.trim().isEmpty()) {
            log.warn("Transaction content is empty.");
            return false;
        }

        Matcher matcher = TRANSFER_CONTENT_PATTERN.matcher(content);
        if (!matcher.find()) {
            log.warn("Transaction content '{}' does not match pattern 'MMO-TOPUP-<userId>'.", content);
            return false;
        }

        String userIdStr = matcher.group(1);
        Long userId;
        try {
            userId = Long.parseLong(userIdStr);
        } catch (NumberFormatException e) {
            log.error("Failed to parse user ID: {}", userIdStr);
            return false;
        }

        // 4. Find user in database
        Optional<User> userOptional = userRepository.findByIdAndIsDeleteFalse(userId);
        if (userOptional.isEmpty()) {
            log.error("User with ID {} not found for top-up.", userId);
            return false;
        }

        User user = userOptional.get();

        // 5. Update user's balance
        Long amount = request.getTransferAmount();
        if (amount == null || amount <= 0) {
            log.error("Invalid top-up amount: {}", amount);
            return false;
        }

        Long oldBalance = user.getBalanceVnd() != null ? user.getBalanceVnd() : 0L;
        user.setBalanceVnd(oldBalance + amount);
        userRepository.save(user);

        // 6. Create top-up transaction record
        TopupTransaction transaction = TopupTransaction.builder()
                .userId(user.getId())
                .amountVnd(amount)
                .sepayCode(sepayCode)
                .status("Success")
                .build();
        topupTransactionRepository.save(transaction);

        log.info("Successfully topped up {} VND for User ID {} ({}). New balance: {}", 
                amount, user.getId(), user.getEmail(), user.getBalanceVnd());

        return true;
    }
}
