package config;

import dal.PermissionRepository;
import dal.UserRepository;
import model.Permission;
import model.User;
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

    public DatabaseSeeder(PermissionRepository permissionRepository, UserRepository userRepository) {
        this.permissionRepository = permissionRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        seedPermissions();
        seedStaffPermissions();
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
                List<String> staffPermNames = Arrays.asList("APPROVE_KYC", "FLAG_SELLER", "APPROVE_WITHDRAWALS", "HANDLE_DISPUTES", "MANAGE_SUPPORT");
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
