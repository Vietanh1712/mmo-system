package controller.mvc;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/")
public class PreOrderPageController {

    @GetMapping("/pre-orders/new")
    public String showPreOrderPage(@RequestParam(value = "productId", required = false) Long productId,
                                   @RequestParam(value = "productName", required = false) String productName,
                                   @RequestParam(value = "sellerName", required = false) String sellerName,
                                   @RequestParam(value = "price", required = false) Long price,
                                   @RequestParam(value = "icon", required = false) String icon,
                                   @RequestParam(value = "iconColor", required = false) String iconColor,
                                   @RequestParam(value = "stock", required = false) Integer stock,
                                   Model model) {
        model.addAttribute("productId", productId);
        model.addAttribute("productName", productName);
        model.addAttribute("sellerName", sellerName);
        model.addAttribute("price", price);
        model.addAttribute("icon", icon);
        model.addAttribute("iconColor", iconColor);
        model.addAttribute("stock", stock);
        return "pre-order-request";
    }

    @GetMapping("/pre-orders")
    public String showPreOrdersListPage() {
        return "pre-orders";
    }
}
