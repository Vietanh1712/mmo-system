package com.mmo.shared.dto;

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
    private Long shopOpeningFees;     // Phí mở Shop (Approved registrations)
    private Long withdrawalFees;      // Phí rút tiền thu được (Completed withdrawals)
    private Long netTotal;            // Doanh thu ròng tổng cộng
}
