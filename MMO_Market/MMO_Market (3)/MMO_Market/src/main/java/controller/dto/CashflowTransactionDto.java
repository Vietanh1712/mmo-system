package controller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CashflowTransactionDto {
    private String id;               // DEP<id> | WTH<id> | TX<id>
    private LocalDateTime timestamp;
    private String email;
    private String type;             // Deposit, Withdrawal, C2C_Purchase
    private Long amount;
    private Long fee;
    private String status;           // Completed, Pending, Failed, Held
}
