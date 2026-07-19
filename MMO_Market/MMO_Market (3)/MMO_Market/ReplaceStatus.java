import java.nio.file.*;
import java.io.IOException;
import java.util.regex.*;

public class ReplaceStatus {
    public static void main(String[] args) throws IOException {
        String path = "d:/mmo-system/MMO_Market/MMO_Market (3)/MMO_Market/apps/frontend/static/js/seller/seller-console.js";
        String content = new String(Files.readAllBytes(Paths.get(path)));
        String func = "\nfunction translateStatus(status) {\n" +
                "    if (!status) return 'Không xác định';\n" +
                "    const s = status.toLowerCase();\n" +
                "    switch (s) {\n" +
                "        case 'active': return 'Đang hoạt động';\n" +
                "        case 'inactive': return 'Ngừng hoạt động';\n" +
                "        case 'pending': return 'Chờ xử lý';\n" +
                "        case 'completed': return 'Hoàn thành';\n" +
                "        case 'held': return 'Đang giữ tiền';\n" +
                "        case 'refunded': return 'Đã hoàn tiền';\n" +
                "        case 'cancelled': return 'Đã hủy';\n" +
                "        case 'failed': return 'Thất bại';\n" +
                "        case 'rejected': return 'Bị từ chối';\n" +
                "        case 'open': return 'Đang mở';\n" +
                "        case 'in_progress': return 'Đang xử lý';\n" +
                "        case 'resolved': return 'Đã giải quyết';\n" +
                "        case 'closed': return 'Đã đóng';\n" +
                "        case 'locked': return 'Đã khóa';\n" +
                "        case 'banned': return 'Bị cấm';\n" +
                "        default: return status;\n" +
                "    }\n" +
                "}\n";

        if (!content.contains("translateStatus")) {
            content = content.replace("function formatVND(value) {", func + "\nfunction formatVND(value) {");
        }

        content = content.replaceAll("\\$\\{([a-zA-Z0-9_]+)\\.status\\}", "\\$\\{translateStatus($1.status)\\}");

        Files.write(Paths.get(path), content.getBytes("UTF-8"));
        System.out.println("Successfully updated statuses in seller-console.js");
    }
}
