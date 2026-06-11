package controller.mvc;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/seller")
public class SellerPageController {

    @GetMapping({"", "/", "/dashboard"})
    public String dashboard() {
        return "seller/dashboard";
    }

    @GetMapping("/shop-info")
    public String shopInfo() {
        return "seller/shop-info";
    }

    @GetMapping("/products/new")
    public String productAdd() {
        return "seller/product-add";
    }

    @GetMapping("/inventory")
    public String inventory() {
        return "seller/inventory";
    }

    @GetMapping("/products/edit")
    public String productEdit() {
        return "seller/product-edit";
    }

    @GetMapping("/variants/new")
    public String variantNew(Model model) {
        model.addAttribute("variantMode", "create");
        return "seller/variant-form";
    }

    @GetMapping("/variants/edit")
    public String variantEdit(Model model) {
        model.addAttribute("variantMode", "edit");
        return "seller/variant-form";
    }

    @GetMapping("/transactions")
    public String transactions() {
        return "seller/transactions";
    }

    @GetMapping("/withdrawals")
    public String withdrawals() {
        return "seller/withdrawals";
    }

    @GetMapping("/withdrawals/detail")
    public String withdrawalDetail() {
        return "seller/withdrawal-detail";
    }

    @GetMapping("/statistics")
    public String statistics() {
        return "seller/statistics";
    }

    @GetMapping("/shop-flags")
    public String shopFlags() {
        return "seller/shop-flags";
    }

    @GetMapping("/reviews")
    public String reviews() {
        return "seller/reviews";
    }

    @GetMapping("/complaints")
    public String complaints() {
        return "seller/complaints";
    }

    @GetMapping("/complaints/detail")
    public String complaintDetail() {
        return "seller/complaint-detail";
    }
}
