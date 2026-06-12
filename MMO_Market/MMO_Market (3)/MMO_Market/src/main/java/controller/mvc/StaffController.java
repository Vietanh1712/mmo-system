package controller.mvc;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/staff")
public class StaffController {
    @GetMapping("/dashboard")
    public String dashboard() {
        return "staff/dashboard";
    }

    @GetMapping("/complaints")
    public String complaints() {
        return "staff/complaints";
    }

    @GetMapping("/complaints/detail")
    public String complaintDetail() {
        return "staff/complaint-detail";
    }

    @GetMapping("/kyc")
    public String kyc() {
        return "staff/kyc";
    }

    @GetMapping("/kyc/detail")
    public String kycDetail() {
        return "staff/kyc-detail";
    }

    @GetMapping("/withdrawals")
    public String withdrawals() {
        return "staff/withdrawals";
    }

    @GetMapping("/withdrawals/detail")
    public String withdrawalDetail() {
        return "staff/withdrawal-detail";
    }

    @GetMapping("/flags")
    public String flags() {
        return "staff/flags";
    }

    @GetMapping("/flags/detail")
    public String flagDetail() {
        return "staff/flag-detail";
    }

    @GetMapping("/chat")
    public String chat() {
        return "staff/chat";
    }
}
