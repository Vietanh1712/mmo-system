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
        
        // Check if query matches a shop name or its aliases to redirect to storefront
        String lowerQuery = query.toLowerCase().trim();
        if (lowerQuery.contains("netflixvn") || lowerQuery.contains("storemaster") || lowerQuery.contains("store master")) {
            return "redirect:/shop/1";
        }
        if (lowerQuery.contains("netflix_vip") || lowerQuery.contains("netflix vip") || lowerQuery.contains("cryptosafe") || lowerQuery.contains("crypto safe")) {
            return "redirect:/shop/2";
        }
        if (lowerQuery.contains("ai_helper") || lowerQuery.contains("ai helper")) {
            return "redirect:/shop/3";
        }
        if (lowerQuery.contains("musiclovers") || lowerQuery.contains("music lovers")) {
            return "redirect:/shop/4";
        }
        if (lowerQuery.contains("microsoft")) {
            return "redirect:/shop/5";
        }
        if (lowerQuery.contains("redpremium") || lowerQuery.contains("red premium")) {
            return "redirect:/shop/6";
        }
        if (lowerQuery.contains("canvapro") || lowerQuery.contains("canva pro") || lowerQuery.contains("designhub") || lowerQuery.contains("design hub")) {
            return "redirect:/shop/7";
        }
        if (lowerQuery.contains("gmailpro") || lowerQuery.contains("gmail pro") || lowerQuery.contains("mailmaster") || lowerQuery.contains("mail master")) {
            return "redirect:/shop/8";
        }
        if (lowerQuery.contains("mmocoder") || lowerQuery.contains("mmo coder") || lowerQuery.contains("mmo_coder")) {
            return "redirect:/shop/9";
        }
        if (lowerQuery.contains("securenet")) {
            return "redirect:/shop/10";
        }
        if (lowerQuery.contains("socialmediaup") || lowerQuery.contains("social media")) {
            return "redirect:/shop/11";
        }
        if (lowerQuery.contains("bannerdesign") || lowerQuery.contains("banner design") || lowerQuery.contains("creativehub") || lowerQuery.contains("creative hub")) {
            return "redirect:/shop/12";
        }

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

    @GetMapping("/shop/{sellerId}")
    public String showShopPage(@PathVariable("sellerId") Long sellerId, Model model) {
        model.addAttribute("sellerId", sellerId);
        return "shop";
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
