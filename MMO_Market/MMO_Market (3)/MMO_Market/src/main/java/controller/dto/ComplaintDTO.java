package controller.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComplaintDTO {

    private Long id;

    private String customerName;

    private String customerEmail;

    private String sellerName;

    private String description;

    private String status;

    private LocalDateTime createdAt;

    private String evidence;

    private String resolution;

    private Long transactionId;

    private String sellerEmail;

    private Long amountVnd;

    private Long commissionVnd;
}