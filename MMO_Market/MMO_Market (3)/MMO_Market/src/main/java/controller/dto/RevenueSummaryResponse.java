package controller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RevenueSummaryResponse {
    private Long commissions;         // Hoa hồng từ Transactions (Completed, Held)
    private Long sellerUpgradeFees;   // Phí nâng cấp Seller (Approved registrations)
    private Long productFeaturedFees; // Phí đẩy tin nổi bật (Active products * Featured Fee)
    private Long withdrawalFees;      // Phí rút tiền thu được (Completed withdrawals)
    private Long netTotal;            // Doanh thu ròng tổng cộng
}
