package controller.mvc;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminMvcController {

    @GetMapping("/users")
    public String userManagementPage() {
        return "admin/users";
    }
}
