package com.mmo.shared.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffDashboardDTO {

    private long openComplaints;

    private long pendingWithdrawals;

    private long shopFlags;

    private long totalComplaints;

    private long pendingKyc;

    private long pendingTransactions;

    private long totalShops;
}