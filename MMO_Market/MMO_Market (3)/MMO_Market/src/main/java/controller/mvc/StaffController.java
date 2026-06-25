package controller.mvc;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import service.StaffDashboardService;
import dal.ComplaintRepository;
import dal.KycRequestRepository;
import dal.TransactionRepository;
import dal.WithdrawalRepository;
import dal.ShopFlagRepository;
import model.KycStatus;

@Controller
@RequestMapping("/staff")
public class StaffController {

    @Autowired
    private ComplaintRepository complaintRepository;

    @Autowired
    private KycRequestRepository kycRequestRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private WithdrawalRepository withdrawalRepository;

    @Autowired
    private ShopFlagRepository shopFlagRepository;

    @Autowired
    private StaffDashboardService staffDashboardService;

    @ModelAttribute
    public void addStaffSidebarCounts(Model model) {
        long complaintCount = complaintRepository.countByStatusAndIsDeleteFalse("InProgress");
        long pendingKycCount = kycRequestRepository.countByStatusAndIsDeleteFalse(KycStatus.PENDING);
        long transactionCount = transactionRepository.countByIsDeleteFalse();
        long withdrawalCount = withdrawalRepository.countByStatusAndIsDeleteFalse("Pending");
        long flagCount = shopFlagRepository.countByIsDeleteFalse();

        model.addAttribute("complaintCount", complaintCount);
        model.addAttribute("pendingKycCount", pendingKycCount);
        model.addAttribute("transactionCount", transactionCount);
        model.addAttribute("withdrawalCount", withdrawalCount);
        model.addAttribute("flagCount", flagCount);
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("dashboard", staffDashboardService.getDashboardData());
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

    @GetMapping("/support-tickets")
    public String supportTickets() {
        return "staff/support-tickets";
    }

    @GetMapping("/support-tickets/detail")
    public String supportTicketDetail() {
        return "staff/support-ticket-detail";
    }

    @GetMapping("/kyc")
    public String kyc() {
        return "staff/kyc";
    }

    @GetMapping("/kyc/detail")
    public String kycDetail() {
        return "staff/kyc-detail";
    }

    @GetMapping("/transactions")
    public String transactions() {
        return "staff/transactions";
    }

    @GetMapping("/transactions/detail")
    public String transactionDetail() {
        return "staff/transaction-detail";
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

    @GetMapping("/shop-registrations")
    public String shopRegistrations() {
        return "staff/shop-registrations";
    }
}
