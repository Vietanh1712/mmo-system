package com.mmo.shared.config;

import com.mmo.shared.dal.CategoryRepository;
import com.mmo.shared.dal.PermissionRepository;
import com.mmo.shared.dal.ProductRepository;
import com.mmo.shared.dal.UserRepository;
import com.mmo.shared.model.Category;
import com.mmo.shared.model.Permission;
import com.mmo.shared.model.Product;
import com.mmo.shared.model.User;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class DatabaseSeeder implements CommandLineRunner {

    private final PermissionRepository permissionRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final JdbcTemplate jdbcTemplate;

    public DatabaseSeeder(PermissionRepository permissionRepository,
                          UserRepository userRepository,
                          CategoryRepository categoryRepository,
                          ProductRepository productRepository,
                          JdbcTemplate jdbcTemplate) {
        this.permissionRepository = permissionRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        fixShopStatusTriggerAndUserRoles();
        seedPermissions();
        seedStaffPermissions();
        seedDefaultCategoriesAndProducts();
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

            try {
                jdbcTemplate.execute("IF NOT EXISTS (SELECT * FROM information_schema.columns WHERE table_name = 'SellerRegistrations' AND column_name = 'fee_vnd') " +
                        "ALTER TABLE SellerRegistrations ADD fee_vnd BIGINT NULL;");
            } catch (Exception ex) {
                System.err.println("DatabaseSeeder: notice adding fee_vnd column: " + ex.getMessage());
            }
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
                .build(),
            Permission.builder()
                .name("MANAGE_CATEGORIES")
                .groupName("Danh mục")
                .description("Cho phép xem, tạo mới, chỉnh sửa và ẩn/hiện các danh mục sản phẩm số trên hệ thống.")
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
                List<String> staffPermNames = Arrays.asList("APPROVE_KYC", "FLAG_SELLER", "APPROVE_WITHDRAWALS", "HANDLE_DISPUTES", "MANAGE_SUPPORT", "MANAGE_SHOPS", "MANAGE_CATEGORIES");
                List<Permission> staffPerms = allPerms.stream()
                        .filter(p -> staffPermNames.contains(p.getName()))
                        .collect(Collectors.toList());
                
                staff.getUserPermissions().addAll(staffPerms);
                userRepository.save(staff);
                System.out.println("Seeded operational permissions for staff01@gmail.com (ID 14)");
            }
        });
    }

    private void seedDefaultCategoriesAndProducts() {
        if (categoryRepository.count() == 0) {
            // 1. Parent Categories matching Marketplace Homepage Search Form
            Category emailCat = createCat("Email", null, "Danh mục tổng hợp các dịch vụ email (Gmail, Hotmail, Outlook, Mail Server...)");
            Category accountCat = createCat("Tài khoản", null, "Danh mục các loại tài khoản Game, Social, AI, Streaming...");
            Category softwareCat = createCat("Phần mềm", null, "Danh mục Key bản quyền phần mềm Windows, Office, Diệt Virus, Đồ họa...");
            Category engagementCat = createCat("Tăng tương tác", null, "Dịch vụ tăng Follow, Like, Sub, View cho các nền tảng MXH.");
            Category softwareServiceCat = createCat("Dịch vụ phần mềm", null, "Tool MMO, Bot Automation, Source Code, Script...");
            Category blockchainCat = createCat("Blockchain", null, "Tài khoản sàn giao dịch Crypto đã KYC, Web3, NFT...");
            Category otherCat = createCat("Khác & Dịch vụ khác", null, "Voucher, Giftcard và các sản phẩm số khác.");

            // 2. Child Categories (Categorized under corresponding Parent Category)
            // Under Email
            Category gmailCat = createCat("Gmail", emailCat, "Tài khoản Gmail ngâm, verified 2FA, có mail khôi phục.");
            Category hotmailCat = createCat("Hotmail / Outlook", emailCat, "Tài khoản Hotmail, Outlook ngâm lâu năm.");
            Category yahooCat = createCat("Yahoo Mail", emailCat, "Tài khoản Yahoo Mail dùng cho công việc.");

            // Under Tài khoản
            Category gameCat = createCat("Tài khoản Game", accountCat, "Tài khoản Valorant, LMHT, Steam, CSGO rank cao.");
            Category socialCat = createCat("Tài khoản Social Media", accountCat, "Tài khoản Facebook, TikTok, Instagram, Twitter/X.");
            Category aiCat = createCat("Tài khoản AI & Công nghệ", accountCat, "Tài khoản ChatGPT Plus, Claude AI, Midjourney.");
            Category streamingCat = createCat("Tài khoản Giải trí & Streaming", accountCat, "Tài khoản Netflix, Spotify, Youtube Premium.");

            // Under Phần mềm
            Category winKeyCat = createCat("Key Windows & Office", softwareCat, "Key kích hoạt Windows 10/11 Pro, Office 365 bản quyền.");
            Category antivirusCat = createCat("Phần mềm Diệt Virus", softwareCat, "Key bản quyền Kaspersky, Avast, Malwarebytes.");

            // Under Tăng tương tác
            Category fbBoostCat = createCat("Tăng Follow & Like Facebook", engagementCat, "Dịch vụ tăng tương tác Facebook an toàn.");
            Category ttBoostCat = createCat("Tăng View & Sub TikTok / Youtube", engagementCat, "Dịch vụ đẩy xu hướng TikTok và kênh Youtube.");

            System.out.println("Seeded default parent & child categories matching Marketplace homepage standard.");

            // 3. Create Sample Products if Seller exists
            Optional<User> sellerOpt = userRepository.findAll().stream()
                    .filter(u -> "SELLER".equalsIgnoreCase(u.getRole()) || "ROLE_SELLER".equalsIgnoreCase(u.getRole()))
                    .findFirst();

            if (sellerOpt.isPresent() && productRepository.count() == 0) {
                User seller = sellerOpt.get();

                Product p1 = new Product();
                p1.setName("Gmail Verified Ngâm > 30 ngày - Có Mail khôi phục");
                p1.setCategory(gmailCat);
                p1.setDescription("Tài khoản Gmail chính chủ ngâm > 30 ngày, bảo hành 1 đổi 1 trong 7 ngày.");
                p1.setSeller(seller);
                p1.setProductType("ACCOUNT");
                p1.setCreatedAt(LocalDateTime.now());
                p1.setIsDelete(false);
                productRepository.save(p1);

                Product p2 = new Product();
                p2.setName("Key Windows 11 Pro Bản Quyền Vĩnh Viễn (1 PC)");
                p2.setCategory(winKeyCat);
                p2.setDescription("Key bản quyền số chính hãng Microsoft, kích hoạt trực tiếp.");
                p2.setSeller(seller);
                p2.setProductType("KEY");
                p2.setCreatedAt(LocalDateTime.now());
                p2.setIsDelete(false);
                productRepository.save(p2);

                Product p3 = new Product();
                p3.setName("Tài khoản Valorant Rank Diamond - Full Skin Reaver");
                p3.setCategory(gameCat);
                p3.setDescription("Tài khoản chính chủ chưa gán SĐT, đổi pass thoải mái.");
                p3.setSeller(seller);
                p3.setProductType("ACCOUNT");
                p3.setCreatedAt(LocalDateTime.now());
                p3.setIsDelete(false);
                productRepository.save(p3);

                System.out.println("Seeded sample products linked to categories.");
            }
        }
    }

    private Category createCat(String name, Category parent, String desc) {
        Category c = new Category();
        c.setName(name);
        c.setParent(parent);
        c.setDescription(desc);
        c.setIsDelete(false);
        c.setCreatedAt(LocalDateTime.now());
        c.setUpdatedAt(LocalDateTime.now());
        return categoryRepository.save(c);
    }
}
