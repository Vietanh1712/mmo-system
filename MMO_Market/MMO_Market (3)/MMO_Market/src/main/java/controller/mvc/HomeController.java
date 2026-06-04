package controller.mvc;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import org.springframework.web.bind.annotation.PathVariable;

@Controller
@RequestMapping("/")
public class HomeController {

    // Trả về trang chủ ngay từ thư mục gốc
    @GetMapping("/")
    public String showHomePage() {
        return "home"; // Trả về template home.html trong thư mục templates/
    }

    @GetMapping("/search")
    public String showSearchPage(@RequestParam(value = "q", required = false) String q,
                                 @RequestParam(value = "keyword", required = false) String keyword,
                                 Model model) {
        String query = q != null ? q : (keyword != null ? keyword : "");
        model.addAttribute("searchQuery", query);
        return "search-results";
    }

    @GetMapping("/products")
    public String showCatalogPage() {
        return "products";
    }

    @GetMapping("/support")
    public String showSupportPage() {
        return "support";
    }

    @GetMapping("/messages")
    public String showMessagesPage() {
        return "messages";
    }

    @GetMapping("/cart")
    public String showCartPage() {
        return "cart";
    }

    @GetMapping("/products/{productId}")
    public String showProductDetailPage(@PathVariable("productId") Long productId, Model model) {
        model.addAttribute("productId", productId);
        return "product-detail";
    }

    @GetMapping("/checkout")
    public String showCheckoutPage(@RequestParam(value = "productId", required = false) Long productId,
                                   @RequestParam(value = "duration", required = false) Integer duration,
                                   Model model) {
        model.addAttribute("productId", productId);
        model.addAttribute("duration", duration != null ? duration : 1);
        return "checkout";
    }
}
