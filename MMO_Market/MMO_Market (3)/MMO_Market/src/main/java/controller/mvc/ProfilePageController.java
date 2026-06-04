package controller.mvc;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ProfilePageController {

    @GetMapping("/profile")
    public String showProfilePage() {
        return "profile/index";
    }

    @GetMapping("/account/kyc")
    public String showKycPage() {
        return "account/kyc";
    }

    @GetMapping("/account/security")
    public String showSecurityPage() {
        return "account/security";
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

    @GetMapping("/account/notifications")
    public String showNotificationsPage() {
        return "account/notifications";
    }
}
