package com.mmo.shared.config;
import com.mmo.shared.model.Transaction;
import com.mmo.shared.model.Chat;

import com.mmo.shared.dal.PermissionRepository;
import com.mmo.shared.dal.UserRepository;
import com.mmo.shared.model.Permission;
import com.mmo.shared.model.User;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class DatabaseSeeder implements CommandLineRunner {

    private final PermissionRepository permissionRepository;
    private final UserRepository userRepository;

    @org.springframework.beans.factory.annotation.Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    public DatabaseSeeder(PermissionRepository permissionRepository, UserRepository userRepository) {
        this.permissionRepository = permissionRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        fixShopStatusTriggerAndUserRoles();
        seedPermissions();
        seedStaffPermissions();
    }

    private void fixShopStatusTriggerAndUserRoles() {
        try {
            jdbcTemplate.execute("UPDATE Users SET role = '{\"role\": \"Customer\"}' WHERE role = 'Customer'");
            jdbcTemplate.execute("UPDATE Users SET role = '{\"role\": \"Customer_Seller\"}' WHERE role = 'Customer_Seller'");
            jdbcTemplate.execute("UPDATE Users SET role = '{\"role\": \"Seller\"}' WHERE role = 'Seller'");
            jdbcTemplate.execute("UPDATE Users SET role = '{\"role\": \"Admin\"}' WHERE role = 'Admin'");
            jdbcTemplate.execute("UPDATE Users SET role = '{\"role\": \"Staff\"}' WHERE role = 'Staff'");

            String sqlTrigger = "CREATE OR ALTER TRIGGER trg_UpdateShopStatus\n" +
                    "ON SellerRegistrations\n" +
                    "AFTER UPDATE\n" +
                    "AS\n" +
                    "BEGIN\n" +
                    "    IF UPDATE(status)\n" +
                    "    BEGIN\n" +
                    "        UPDATE Users\n" +
                    "        SET shop_status = i.status,\n" +
                    "            role = CASE\n" +
                    "                WHEN i.status = 'Approved' THEN\n" +
                    "                    CASE\n" +
                    "                        WHEN ISJSON(Users.role) = 1 AND JSON_VALUE(Users.role, '$.role') = 'Customer' THEN '{\"role\": \"Customer_Seller\"}'\n" +
                    "                        WHEN Users.role = 'Customer' THEN 'Customer_Seller'\n" +
                    "                        ELSE Users.role\n" +
                    "                    END\n" +
                    "                WHEN i.status = 'Rejected' THEN\n" +
                    "                    CASE\n" +
                    "                        WHEN ISJSON(Users.role) = 1 AND JSON_VALUE(Users.role, '$.role') = 'Customer_Seller' THEN '{\"role\": \"Customer\"}'\n" +
                    "                        WHEN Users.role = 'Customer_Seller' THEN 'Customer'\n" +
                    "                        ELSE Users.role\n" +
                    "                    END\n" +
                    "                ELSE Users.role\n" +
                    "            END\n" +
                    "        FROM Users\n" +
                    "        INNER JOIN inserted i ON Users.id = i.user_id\n" +
                    "        INNER JOIN deleted d ON i.id = d.id\n" +
                    "        WHERE i.status IN ('Approved', 'Rejected') AND i.status != d.status;\n" +
                    "    END\n" +
                    "END;";
            jdbcTemplate.execute(sqlTrigger);
            System.out.println("Auto-fixed SQL Server trigger trg_UpdateShopStatus and user roles.");
        } catch (Exception e) {
            System.err.println("DatabaseSeeder: notice fixing trigger: " + e.getMessage());
        }
    }

    private void seedPermissions() {
        List<Permission> defaultPermissions = Arrays.asList(
            Permission.builder()
                .name("APPROVE_KYC")
                .groupName("Kiểm duyệt")
                .description("Cho phép xem, duyệt hoặc từ chối thông tin định danh cá nhân của người dùng.")
                .build(),
            Permission.builder()
                .name("FLAG_SELLER")
                .groupName("Kiểm duyệt")
                .description("Cho phép gắn cờ vi phạm (gạch phạt) đối với người bán vi phạm chính sách.")
                .build(),
            Permission.builder()
                .name("APPROVE_WITHDRAWALS")
                .groupName("Tài chính")
                .description("Cho phép duyệt lệnh chuyển tiền/rút tiền của Seller từ ví hệ thống về tài khoản ngân hàng.")
                .build(),
            Permission.builder()
                .name("HANDLE_DISPUTES")
                .groupName("Vận hành")
                .description("Cho phép làm trung gian giải quyết khiếu nại giữa người mua và người bán, hoàn trả hoặc giải ngân tiền Escrow.")
                .build(),
            Permission.builder()
                .name("MANAGE_SUPPORT")
                .groupName("Vận hành")
                .description("Cho phép tiếp nhận, phản hồi và hỗ trợ giải đáp các thắc mắc (ticketing/live chat) của khách hàng.")
                .build(),
            Permission.builder()
                .name("MANAGE_SHOPS")
                .groupName("Vận hành")
                .description("Cho phép xem, phê duyệt yêu cầu mở gian hàng, khóa hoặc mở khóa hoạt động của các Shop.")
                .build()
        );

        for (Permission defaultPerm : defaultPermissions) {
            if (permissionRepository.findByName(defaultPerm.getName()).isEmpty()) {
                permissionRepository.save(defaultPerm);
                System.out.println("Seeded default permission: " + defaultPerm.getName());
            }
        }
    }

    private void seedStaffPermissions() {
        userRepository.findByIdWithPermissions(14L).ifPresent(staff -> {
            if (staff.getUserPermissions() == null || staff.getUserPermissions().isEmpty()) {
                List<Permission> allPerms = permissionRepository.findAll();
                if (staff.getUserPermissions() == null) {
                    staff.setUserPermissions(new java.util.HashSet<>());
                }
                List<String> staffPermNames = Arrays.asList("APPROVE_KYC", "FLAG_SELLER", "APPROVE_WITHDRAWALS", "HANDLE_DISPUTES", "MANAGE_SUPPORT", "MANAGE_SHOPS");
                List<Permission> staffPerms = allPerms.stream()
                        .filter(p -> staffPermNames.contains(p.getName()))
                        .collect(Collectors.toList());
                
                staff.getUserPermissions().addAll(staffPerms);
                userRepository.save(staff);
                System.out.println("Seeded operational permissions for staff01@gmail.com (ID 14)");
            }
        });
    }
}
