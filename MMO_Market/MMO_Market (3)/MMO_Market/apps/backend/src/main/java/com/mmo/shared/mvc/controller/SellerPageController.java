package com.mmo.shared.mvc.controller;
import com.mmo.shared.model.Withdrawal;
import com.mmo.shared.model.Complaint;
import com.mmo.shared.model.Product;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Controller phục vụ các trang giao diện (MVC view) dành cho Người bán (Seller Portal).
 * Quản lý điều hướng tới các view HTML liên quan đến hoạt động của Shop như Dashboard,
 * thông tin cửa hàng, quản lý sản phẩm, giao dịch, rút tiền, khiếu nại, thống kê doanh thu,...
 */
@Controller
@RequestMapping("/seller")
public class SellerPageController {

    /**
     * Hiển thị trang Dashboard (thống kê tổng quan) của Người bán.
     */
    @GetMapping({"", "/", "/dashboard"})
    public String dashboard() {
        return "seller/dashboard";
    }

    /**
     * Hiển thị trang quản lý thông tin cửa hàng (Shop Info), bao gồm cả thông tin tài khoản ngân hàng.
     */
    @GetMapping("/shop-info")
    public String shopInfo() {
        return "seller/shop-info";
    }

    /**
     * Hiển thị trang đóng cửa hàng (Close Shop / Tạm ngưng hoạt động).
     */
    @GetMapping("/close-shop")
    public String closeShop() {
        return "seller/close-shop";
    }

    /**
     * Hiển thị trang đăng bán sản phẩm mới.
     */
    @GetMapping("/products/new")
    public String productAdd() {
        return "seller/product-add";
    }

    /**
     * Hiển thị trang quản lý kho hàng (Inventory) - liệt kê sản phẩm và biến thể.
     */
    @GetMapping("/inventory")
    public String inventory() {
        return "seller/inventory";
    }

    /**
     * Hiển thị trang chỉnh sửa thông tin sản phẩm.
     */
    @GetMapping("/products/edit")
    public String productEdit() {
        return "seller/product-edit";
    }

    /**
     * Hiển thị trang thêm mới biến thể sản phẩm.
     */
    @GetMapping("/variants/new")
    public String variantNew(Model model) {
        model.addAttribute("variantMode", "create");
        return "seller/variant-form";
    }

    /**
     * Hiển thị trang chỉnh sửa thông tin biến thể sản phẩm.
     */
    @GetMapping("/variants/edit")
    public String variantEdit(Model model) {
        model.addAttribute("variantMode", "edit");
        return "seller/variant-form";
    }

    /**
     * Hiển thị trang quản lý và theo dõi lịch sử giao dịch bán hàng (đơn hàng).
     */
    @GetMapping("/transactions")
    public String transactions() {
        return "seller/transactions";
    }

    /**
     * Hiển thị trang quản lý và gửi yêu cầu rút tiền từ số dư Shop.
     */
    @GetMapping("/withdrawals")
    public String withdrawals() {
        return "seller/withdrawals";
    }

    /**
     * Hiển thị trang chi tiết một yêu cầu rút tiền cụ thể (bao gồm lý do từ chối nếu có, hoặc minh chứng).
     */
    @GetMapping("/withdrawals/detail")
    public String withdrawalDetail() {
        return "seller/withdrawal-detail";
    }

    /**
     * Hiển thị trang báo cáo thống kê doanh thu theo thời gian và top sản phẩm bán chạy.
     */
    @GetMapping("/statistics")
    public String statistics() {
        return "seller/statistics";
    }

    /**
     * Hiển thị trang danh sách cảnh cáo (Shop Flags) từ Ban quản trị sàn đối với Shop.
     */
    @GetMapping("/shop-flags")
    public String shopFlags() {
        return "seller/shop-flags";
    }

    /**
     * Hiển thị trang quản lý các đánh giá (Reviews) của khách hàng về sản phẩm của Shop.
     */
    @GetMapping("/reviews")
    public String reviews() {
        return "seller/reviews";
    }

    /**
     * Hiển thị trang quản lý và xử lý các khiếu nại (Complaints) từ khách hàng.
     */
    @GetMapping("/complaints")
    public String complaints() {
        return "seller/complaints";
    }

    /**
     * Hiển thị trang chi tiết khiếu nại và khung chat đối chất ba bên (Khách hàng - Người bán - Staff).
     */
    @GetMapping("/complaints/detail")
    public String complaintDetail() {
        return "seller/complaint-detail";
    }

    /**
     * Hiển thị trang quản lý đơn đặt hàng trước (Pre-orders).
     */
    @GetMapping("/preorders")
    public String preOrders() {
        return "seller/preorders";
    }
}
