package com.mmo.feature.admin.controller.mvc;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminPageController {

    @GetMapping("/admin/users")
    public String showAdminUsersPage() {
        return "admin/users";
    }
}
