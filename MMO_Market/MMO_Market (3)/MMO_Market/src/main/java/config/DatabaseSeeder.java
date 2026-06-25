package config;

import dal.PermissionRepository;
import model.Permission;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class DatabaseSeeder implements CommandLineRunner {

    private final PermissionRepository permissionRepository;

    public DatabaseSeeder(PermissionRepository permissionRepository) {
        this.permissionRepository = permissionRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        seedPermissions();
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
}
