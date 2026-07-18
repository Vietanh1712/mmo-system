package com.mmo.shared.mvc.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ProfilePageController {

    @GetMapping("/profile")
    public String showProfilePage() {
        return "profile/index";
    }

    @GetMapping("/account/kyc")
    public String showKycPage(Authentication authentication) {
        if (authentication != null) {
            boolean isSystem = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_STAFF") || a.getAuthority().equals("ROLE_ADMIN"));
            if (isSystem) {
                return "redirect:/profile";
            }
        }
        return "account/kyc";
    }

    @GetMapping("/account/security")
    public String showSecurityPage() {
        return "account/security";
    }

    @GetMapping("/account/register-shop")
    public String showRegisterShopPage() {
        return "account/register-shop";
    }

    @GetMapping("/wallet")
    public String showWalletPage() {
        return "account/wallet";
    }

    @GetMapping("/wallet/topup")
    public String showWalletTopupPage() {
        return "account/topup";
    }

    @GetMapping("/wallet/transactions")
    public String showWalletTransactionsPage() {
        return "account/transactions";
    }

    @GetMapping("/account/orders")
    public String showMyOrdersPage() {
        return "account/orders";
    }

    @GetMapping("/account/orders/{orderCode}")
    public String showOrderDetailPage() {
        return "account/order-detail";
    }

    @GetMapping("/account/orders/{orderCode}/feedback")
    public String showLeaveFeedbackPage() {
        return "account/leave-feedback";
    }

    @GetMapping("/account/notifications")
    public String showNotificationsPage() {
        return "account/notifications";
    }

    @GetMapping("/account/tickets")
    public String showMyTicketsPage() {
        return "account/tickets";
    }

    @GetMapping("/account/complaints")
    public String showMyComplaintsPage() {
        return "account/complaints";
    }

    @GetMapping("/account/complaints/detail")
    public String showComplaintDetailPage() {
        return "account/complaint-detail";
    }
}
