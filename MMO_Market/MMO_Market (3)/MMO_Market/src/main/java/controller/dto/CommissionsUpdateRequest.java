package controller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommissionsUpdateRequest {
    private Double basePercent;
    private Long flatBuyerFee;
    private Double withdrawalPercent;
    private Long minWithdrawFee;
    private Long minWithdrawLimit;
    private Long maxWithdrawLimit;
    private Long autoWithdrawLimit;
    private Long minDepositLimit;
}
