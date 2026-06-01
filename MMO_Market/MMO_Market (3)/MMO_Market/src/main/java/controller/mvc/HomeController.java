package controller.mvc;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/")
public class HomeController {

    // Trả về trang chủ ngay từ thư mục gốc
    @GetMapping("/")
    public String showHomePage() {
        return "home"; // Trả về template home.html trong thư mục templates/
    }
}