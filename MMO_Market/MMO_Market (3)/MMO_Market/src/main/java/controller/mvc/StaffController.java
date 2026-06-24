package controller.mvc;

import controller.dto.ComplaintDTO;
import controller.dto.StaffDashboardDTO;
import dal.*;
import model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import service.ComplaintService;
import service.StaffDashboardService;
import org.springframework.ui.Model;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import java.util.List;
import java.time.LocalDate;
import java.time.LocalDateTime;
@Controller
@RequestMapping("/staff")
public class StaffController {

    @Autowired
    private StaffDashboardService staffDashboardService;

    @Autowired
    private ComplaintRepository complaintRepository;

    @Autowired
    private WithdrawalRepository withdrawalRepository;

    @Autowired
    private ShopFlagRepository shopFlagRepository;

    @Autowired
    private KYCRequestRepository kycRequestRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private ComplaintService complaintService;




    @ModelAttribute
    public void addSidebarData(Model model) {

        model.addAttribute(
                "complaintCount",
                complaintRepository.countByStatusAndIsDeleteFalse("InProgress"));

        model.addAttribute(
                "pendingKycCount",
                kycRequestRepository.countByStatus("Pending"));

        model.addAttribute(
                "transactionCount",
                transactionRepository.countByStatusAndIsDeleteFalse("Pending"));

        model.addAttribute(
                "withdrawalCount",
                withdrawalRepository.countByStatusAndIsDeleteFalse("Pending"));

        model.addAttribute(
                "flagCount",
                shopFlagRepository.countByIsDeleteFalse());
    }

    @GetMapping("/dashboard")

    public String dashboard(Model model) {

        StaffDashboardDTO dashboard =
                staffDashboardService.getDashboardData();

        model.addAttribute("dashboard", dashboard);

        return "staff/dashboard";
    }

    @GetMapping("/complaints")
    public String complaints(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            Model model
    ) {

        model.addAttribute(
                "complaints",
                complaintService.getAllComplaints());

        model.addAttribute(
                "totalComplaints",
                complaintService.getTotalComplaints());

        model.addAttribute(
                "inProgressComplaints",
                complaintService.getInProgressComplaints());

        model.addAttribute(
                "resolvedComplaints",
                complaintService.getResolvedComplaints());

        model.addAttribute(
                "refusedComplaints",
                complaintService.getRefusedComplaints());


        Page<ComplaintDTO> complaintPage =
                complaintService.getComplaints(
                        page,
                        keyword,
                        status);

        model.addAttribute(
                "complaintPage",
                complaintPage);

        model.addAttribute(
                "complaints",
                complaintPage.getContent());

        model.addAttribute(
                "currentPage",
                page);

        model.addAttribute(
                "totalPages",
                complaintPage.getTotalPages());

        model.addAttribute(
                "keyword",
                keyword);

        model.addAttribute(
                "selectedStatus",
                status);

        model.addAttribute(
                "statuses",
                complaintService.getAllStatuses());


        return "staff/complaints";
    }

    @GetMapping("/complaints/detail")
    public String complaintDetail(@RequestParam Long id,
                                  Model model) {
        ComplaintDTO complaint =
                complaintService.getComplaintById(id);

        model.addAttribute(
                "complaint",
                complaint);

        return "staff/complaint-detail";
    }

    @PostMapping("/complaints/process")
    public String processComplaint(
            @RequestParam Long id,
            @RequestParam String status,
            @RequestParam String resolution,
            RedirectAttributes redirectAttributes) {

        complaintService.processComplaint(
                id,
                status,
                resolution);

        redirectAttributes.addFlashAttribute(
                "success",
                "Cập nhật khiếu nại thành công");

        return "redirect:/staff/complaints/detail?id=" + id;
    }


    @PostMapping("/complaints/reject")
    public String rejectComplaint(
            @RequestParam Long id,
            RedirectAttributes redirectAttributes) {

        redirectAttributes.addFlashAttribute(
                "warning",
                "Từ chối thay đổi thành công");

        return "redirect:/staff/complaints/detail?id=" + id;
    }


    @GetMapping("/kyc")
    public String kyc(

            @RequestParam(defaultValue = "0") int page,

            @RequestParam(required = false) String keyword,

            @RequestParam(required = false) String status,

            @RequestParam(required = false) String typeKyc,

            Model model
    ) {


        if(keyword != null){
            keyword = keyword.trim();

            if(keyword.isEmpty()){
                keyword = null;
            }
        }


        if(status != null && status.isEmpty()){
            status = null;
        }


        if(typeKyc != null && typeKyc.isEmpty()){
            typeKyc = null;
        }



        Pageable pageable =
                PageRequest.of(
                        page,
                        4,
                        Sort.by("id").ascending()
                );



        Page<KYCRequest> result;



    /*
       filter:
       id + name + status + type
    */


        if(keyword != null
                && status != null
                && typeKyc != null){


            try{

                Long id =
                        Long.parseLong(keyword);


                result =
                        kycRequestRepository
                                .findByIdAndStatusAndTypeKyc(
                                        id,
                                        status,
                                        typeKyc,
                                        pageable
                                );


            }catch(Exception e){


                result =
                        kycRequestRepository
                                .findByFullNameContainingIgnoreCaseAndStatusAndTypeKyc(
                                        keyword,
                                        status,
                                        typeKyc,
                                        pageable
                                );
            }



        }

        else if(keyword != null && status != null){


            try{

                Long id =
                        Long.parseLong(keyword);


                result =
                        kycRequestRepository
                                .findByIdAndStatus(
                                        id,
                                        status,
                                        pageable);


            }catch(Exception e){

                result =
                        kycRequestRepository
                                .findByFullNameContainingIgnoreCaseAndStatus(
                                        keyword,
                                        status,
                                        pageable);
            }

        }


        else if(keyword != null && typeKyc != null){


            try{

                Long id =
                        Long.parseLong(keyword);


                result =
                        kycRequestRepository
                                .findByIdAndTypeKyc(
                                        id,
                                        typeKyc,
                                        pageable);


            }catch(Exception e){

                result =
                        kycRequestRepository
                                .findByFullNameContainingIgnoreCaseAndTypeKyc(
                                        keyword,
                                        typeKyc,
                                        pageable);
            }

        }



        else if(status != null && typeKyc != null){


            result =
                    kycRequestRepository
                            .findByStatusAndTypeKyc(
                                    status,
                                    typeKyc,
                                    pageable);

        }


        else if(keyword != null){


            try{

                Long id =
                        Long.parseLong(keyword);


                result =
                        kycRequestRepository
                                .findById(id,pageable);


            }catch(Exception e){


                result =
                        kycRequestRepository
                                .findByFullNameContainingIgnoreCase(
                                        keyword,
                                        pageable);

            }

        }



        else if(status != null){


            result =
                    kycRequestRepository
                            .findByStatus(
                                    status,
                                    pageable);

        }



        else if(typeKyc != null){


            result =
                    kycRequestRepository
                            .findByTypeKyc(
                                    typeKyc,
                                    pageable);

        }



        else{


            result =
                    kycRequestRepository
                            .findAll(pageable);

        }



        model.addAttribute(
                "kycs",
                result.getContent()
        );


        model.addAttribute(
                "currentPage",
                page
        );


        model.addAttribute(
                "totalPages",
                result.getTotalPages()
        );



        model.addAttribute(
                "keyword",
                keyword
        );


        model.addAttribute(
                "selectedStatus",
                status
        );


        model.addAttribute(
                "selectedTypeKyc",
                typeKyc
        );



        // combobox lấy database


        model.addAttribute(
                "statuses",
                kycRequestRepository
                        .findAll()
                        .stream()
                        .map(KYCRequest::getStatus)
                        .distinct()
                        .toList()
        );


        model.addAttribute(
                "types",
                kycRequestRepository
                        .findAll()
                        .stream()
                        .map(KYCRequest::getTypeKyc)
                        .distinct()
                        .toList()
        );



        model.addAttribute(
                "totalKyc",
                kycRequestRepository.count()
        );


        model.addAttribute(
                "pendingKyc",
                kycRequestRepository.countByStatus("Pending")
        );


        model.addAttribute(
                "approvedKyc",
                kycRequestRepository.countByStatus("Approved")
        );


        model.addAttribute(
                "rejectedKyc",
                kycRequestRepository.countByStatus("Rejected")
        );



        return "staff/kyc";
    }

    @GetMapping("/kyc/detail")
    public String kycDetail(
            @RequestParam Long id,
            Model model
    ) {

        KYCRequest kyc =
                kycRequestRepository.findById(id)
                        .orElse(null);

        if (kyc == null) {
            return "redirect:/staff/kyc";
        }

        model.addAttribute(
                "kyc",
                kyc
        );

        return "staff/kyc-detail";
    }

    @PostMapping("/kyc/approve")
    public String approveKyc(
            @RequestParam Long id,
            RedirectAttributes redirectAttributes
    ) {

        KYCRequest kyc =
                kycRequestRepository
                        .findById(id)
                        .orElse(null);

        if (kyc != null) {

            kyc.setStatus("Approved");

            kyc.setReviewedAt(
                    LocalDateTime.now()
            );

            kycRequestRepository.save(kyc);

            redirectAttributes.addFlashAttribute(
                    "success",
                    "Phê duyệt hồ sơ thành công"
            );
        }

        return "redirect:/staff/kyc/detail?id=" + id;
    }


    @PostMapping("/kyc/reject")
    public String rejectKyc(
            @RequestParam Long id,
            @RequestParam String reason,
            RedirectAttributes redirectAttributes
    ) {

        KYCRequest kyc =
                kycRequestRepository
                        .findById(id)
                        .orElse(null);

        if (kyc != null) {

            kyc.setStatus("Rejected");

            kyc.setRejectionReason(reason);

            kyc.setReviewedAt(
                    LocalDateTime.now()
            );

            kycRequestRepository.save(kyc);

            redirectAttributes.addFlashAttribute(
                    "success",
                    "Đã từ chối hồ sơ KYC"
            );
        }

        return "redirect:/staff/kyc/detail?id=" + id;
    }


    @GetMapping("/transactions")
    public String transactions(

            Model model,

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(required = false)
            String keyword,

            @RequestParam(required = false)
            String type,

            @RequestParam(required = false)
            String status,

            @RequestParam(required = false)
            String fromDate,

            @RequestParam(required = false)
            String toDate

    ) {

        // keyword
        if (keyword != null) {

            keyword = keyword.trim();

            if (keyword.isEmpty()) {
                keyword = null;
            }
        }

        Long id = null;

        if(keyword != null){

            try {
                id = Long.parseLong(keyword);
            }catch(Exception e){
                id = null;
            }

        }

        // type
        if (type != null) {

            type = type.trim();

            if (type.isEmpty() || type.equalsIgnoreCase("ALL")) {
                type = null;
            }
        }

        // status
        if (status != null) {

            status = status.trim();

            if (status.isEmpty() || status.equalsIgnoreCase("ALL")) {
                status = null;
            }
        }


        LocalDateTime from = null;
        LocalDateTime to = null;

        try {

            if (fromDate != null && !fromDate.isBlank()) {

                from =
                        LocalDate.parse(fromDate)
                                .atStartOfDay();
            }

            if (toDate != null && !toDate.isBlank()) {

                to =
                        LocalDate.parse(toDate)
                                .atTime(23, 59, 59);
            }

        } catch (Exception e) {

            from = null;
            to = null;
        }


        Pageable pageable =
                PageRequest.of(
                        page,
                        4,
                        Sort.by("createdAt").descending()
                );


        Page<Transaction> transactionPage;

        if(keyword == null
                && type == null
                && status == null
                && from == null
                && to == null){

            transactionPage =
                    transactionRepository.findAll(pageable);

        }else{

            transactionPage =
                    transactionRepository.searchTransactions(
                            keyword,
                            id,
                            type,
                            status,
                            from,
                            to,
                            pageable
                    );
        }


        System.out.println(
                "Result size = "
                        + transactionPage.getContent().size()
        );


        System.out.println("FROM = " + from);
        System.out.println("TO = " + to);

        transactionPage.getContent().forEach(t -> {
            System.out.println(
                    "ID = " + t.getId()
                            + " CREATED = "
                            + t.getCreatedAt()
            );
        });


        model.addAttribute(
                "transactions",
                transactionPage.getContent()
        );

        model.addAttribute(
                "currentPage",
                page
        );

        model.addAttribute(
                "totalPages",
                transactionPage.getTotalPages()
        );

        model.addAttribute(
                "keyword",
                keyword
        );

        model.addAttribute(
                "selectedType",
                type
        );

        model.addAttribute(
                "selectedStatus",
                status
        );

        model.addAttribute(
                "fromDate",
                fromDate
        );

        model.addAttribute(
                "toDate",
                toDate
        );


        // combobox
        model.addAttribute(
                "types",
                transactionRepository.findAllTransactionTypes()
        );

        model.addAttribute(
                "statuses",
                transactionRepository.findAllStatus()
        );


        // statistics
        model.addAttribute(
                "totalTransactions",
                transactionRepository.countByIsDeleteFalse()
        );

        model.addAttribute(
                "completedTransactions",
                transactionRepository.countByStatusAndIsDeleteFalse(
                        "Completed"
                )
        );

        model.addAttribute(
                "pendingTransactions",
                transactionRepository.countByStatusAndIsDeleteFalse(
                        "Pending"
                )
        );

        model.addAttribute(
                "failTransactions",
                transactionRepository.countByStatusAndIsDeleteFalse(
                        "Fail"
                )
        );


        return "staff/transactions";
    }

    @GetMapping("/transactions/detail")
    public String transactionDetail(
            @RequestParam Long id,
            Model model
    ) {

        Transaction transaction =
                transactionRepository.findDetailById(id);

        if (transaction == null) {
            return "redirect:/staff/transactions";
        }

        Complaint complaint =
                complaintRepository.findByTransactionId(
                        transaction.getId()
                );

        model.addAttribute(
                "transaction",
                transaction
        );

        model.addAttribute(
                "complaint",
                complaint
        );


        long remainingHours = 0;

        if (transaction.getEscrowReleaseDate() != null) {

            remainingHours =
                    java.time.Duration.between(
                            LocalDateTime.now(),
                            transaction.getEscrowReleaseDate()
                    ).toHours();

            if (remainingHours < 0) {
                remainingHours = 0;
            }
        }

        model.addAttribute(
                "remainingHours",
                remainingHours
        );

        return "staff/transaction-detail";
    }

    @GetMapping("/withdrawals")
    public String withdrawals(
Model model,
@RequestParam(defaultValue = "0")
int page,
@RequestParam(required = false)
String status,
@RequestParam(required = false)
String keyword,
@RequestParam(required = false)
String minAmount
    ) {
        // status
        if (status != null) {
            status = status.trim();
            if (status.isEmpty() || status.equalsIgnoreCase("ALL")) {
                status = null;
            }
        }

// keyword
        if (keyword != null) {
            keyword = keyword.trim();

            if (keyword.isEmpty()) {
                keyword = null;
            }
        }

// amount
        Long amountFilter = null;

        if (minAmount != null) {

            minAmount = minAmount.trim();

            if (!minAmount.isEmpty()) {

                try {
                    amountFilter = Long.parseLong(minAmount);
                } catch (Exception e) {
                    amountFilter = null;
                }

            }
        }
        Pageable pageable =
                PageRequest.of(
                        page,
                        4,
                        Sort.by("id").ascending()
                );


        Page<Withdrawal> withdrawalPage;

        if (status == null
                && keyword == null
                && amountFilter == null) {

            withdrawalPage =
                    withdrawalRepository.findAll(pageable);

        } else {

            withdrawalPage =
                    withdrawalRepository.searchWithdrawals(
                            status,
                            keyword,
                            amountFilter,
                            pageable
                    );
        }
        System.out.println(
                "Result size = "
                        + withdrawalPage.getContent().size()
        );


        model.addAttribute(
                "withdrawals",
                withdrawalPage.getContent()
        );

        model.addAttribute(
                "currentPage",
                page
        );

        model.addAttribute(
                "totalPages",
                withdrawalPage.getTotalPages()
        );

        model.addAttribute(
                "keyword",
                keyword
        );

        model.addAttribute(
                "selectedStatus",
                status
        );

        model.addAttribute(
                "minAmount",
                minAmount
        );

        model.addAttribute(
                "statuses",
                withdrawalRepository.findAllStatus()
        );

        model.addAttribute(
                "totalWithdrawals",
                withdrawalRepository.count()
        );

        model.addAttribute(
                "pendingWithdrawals",
                withdrawalRepository.countByStatusAndIsDeleteFalse(
                        "Pending"
                )
        );

        model.addAttribute(
                "completedWithdrawals",
                withdrawalRepository.countByStatusAndIsDeleteFalse(
                        "Approved"
                )
        );

        model.addAttribute(
                "rejectedWithdrawals",
                withdrawalRepository.countByStatusAndIsDeleteFalse(
                        "Rejected"
                )
        );
        return "staff/withdrawals";
    }

    @GetMapping("/withdrawals/detail")
    public String withdrawalDetail(
            @RequestParam Long id,
            Model model
    ) {
        Withdrawal withdrawal =
                withdrawalRepository
                        .findDetailById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Withdrawal not found"));

        model.addAttribute(
                "withdrawal",
                withdrawal);

        return "staff/withdrawal-detail";
    }

    @PostMapping("/withdrawals/update-status")
    public String updateWithdrawalStatus(
            @RequestParam Long id,
            @RequestParam String status,
            RedirectAttributes redirectAttributes
    ) {

        Withdrawal withdrawal =
                withdrawalRepository
                        .findById(id)
                        .orElseThrow();

        withdrawal.setStatus(status);

        withdrawalRepository.save(withdrawal);

        redirectAttributes.addFlashAttribute(
                "success",
                "Cập nhật trạng thái thành công"
        );

        return "redirect:/staff/withdrawals/detail?id=" + id;
    }


    @PostMapping("/withdrawals/reject")
    public String rejectWithdrawal(
            @RequestParam Long id,
            RedirectAttributes ra) {

        ra.addFlashAttribute(
                "warning",
                "Đã từ chối rút tiền");

        return "redirect:/staff/withdrawals/detail?id=" + id;
    }

    @GetMapping("/flags")
    public String flags(
            Model model,

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(required = false)
            String keyword,

            @RequestParam(required = false)
            String level,

            @RequestParam(required = false)
            String status
    ) {

        if(keyword != null){
            keyword = keyword.trim();

            if(keyword.equals("")){
                keyword = null;
            }
        }


        if(level != null){
            level = level.trim();

            if(level.equals("") || level.equalsIgnoreCase("ALL")){
                level = null;
            }
        }


        if(status != null){
            status = status.trim();

            if(status.equals("") || status.equalsIgnoreCase("ALL")){
                status = null;
            }
        }



        Pageable pageable =
                PageRequest.of(
                        page,
                        4,
                        Sort.by("createdAt").descending()
                );

        Page<ShopFlag> flagPage;


        if(keyword == null
                && level == null
                && status == null){

            flagPage =
                    shopFlagRepository.findAllByIsDeleteFalse(pageable);

        }else{

            flagPage =
                    shopFlagRepository.searchFlags(
                            keyword,
                            level,
                            status,
                            pageable
                    );
        }


        System.out.println(
                "SEARCH => keyword="
                        +keyword
                        +" level="
                        +level
                        +" status="
                        +status
        );

        model.addAttribute(
                "flags",
                flagPage.getContent()
        );


        model.addAttribute(
                "currentPage",
                page
        );


        model.addAttribute(
                "totalPages",
                flagPage.getTotalPages()
        );



        model.addAttribute(
                "totalFlags",
                flagPage.getTotalElements()
        );



        model.addAttribute(
                "keyword",
                keyword
        );


        model.addAttribute(
                "selectedLevel",
                level
        );


        model.addAttribute(
                "selectedStatus",
                status
        );



        model.addAttribute(
                "levels",
                List.of(
                        "Warning",
                        "Danger",
                        "Critical"
                )
        );


        model.addAttribute(
                "statuses",
                List.of(
                        "Effect",
                        "Removed"
                )
        );



        model.addAttribute(
                "dangerFlags",
                shopFlagRepository
                        .countByFlagLevelAndIsDeleteFalse("Danger")
        );


        model.addAttribute(
                "warningFlags",
                shopFlagRepository
                        .countByFlagLevelAndIsDeleteFalse("Warning")
        );


        model.addAttribute(
                "criticalFlags",
                shopFlagRepository
                        .countByFlagLevelAndIsDeleteFalse("Critical")
        );



        model.addAttribute(
                "activeFlags",
                shopFlagRepository
                        .countByStatusAndIsDeleteFalse("Effect")
        );


        model.addAttribute(
                "removedFlags",
                shopFlagRepository
                        .countByStatusAndIsDeleteFalse("Removed")
        );

        return "staff/flags";
    }

    @GetMapping("/flags/detail")
    public String flagDetail(
            @RequestParam Long id,
            Model model
    ) {

        ShopFlag flag =
                shopFlagRepository
                        .findByIdAndIsDeleteFalse(id)
                        .orElse(null);

        if (flag == null) {
            return "redirect:/staff/flags";
        }

        model.addAttribute("flag", flag);

        return "staff/flag-detail";
    }


    @PostMapping("/flags/update")
    public String updateFlag(
            @RequestParam Long id,
            @RequestParam String flagLevel,
            @RequestParam String status,
            @RequestParam String reason,
            RedirectAttributes redirectAttributes) {

        ShopFlag flag =
                shopFlagRepository
                        .findByIdAndIsDeleteFalse(id)
                        .orElse(null);

        if (flag != null) {

            flag.setFlagLevel(flagLevel);
            flag.setStatus(status);
            flag.setReason(reason);

            shopFlagRepository.save(flag);

            redirectAttributes.addFlashAttribute(
                    "success",
                    "Cập nhật cờ thành công");
        }

        return "redirect:/staff/flags/detail?id=" + id;
    }

    @PostMapping("/flags/remove")
    public String removeFlag(
            @RequestParam Long id,
            RedirectAttributes redirectAttributes) {

        ShopFlag flag =
                shopFlagRepository
                        .findByIdAndIsDeleteFalse(id)
                        .orElse(null);

        if (flag != null) {

            flag.setIsDelete(true);

            shopFlagRepository.save(flag);

            redirectAttributes.addFlashAttribute(
                    "success",
                    "Đã gỡ cờ thành công");
        }

        return "redirect:/staff/flags";
    }



    @GetMapping("/chat")
    public String chat() {
        return "staff/chat";
    }
}
