import java.sql.Connection;
import java.sql.DriverManager;
import org.junit.jupiter.api.Test; // Dùng Jupiter (JUnit 5)
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class DBTest {
    @Test
    void testConnection() {
        try {
            // Chuỗi kết nối của bạn
// Thay đổi URL này (bỏ databaseName nếu cần hoặc viết đúng tên trong SQL Server)
            String url = "jdbc:sqlserver://localhost:1433;databaseName=MMO_System;encrypt=true;trustServerCertificate=true;loginTimeout=30;";            String user = "sa";
            String pass = "123";

            // JDBC 4.0 trở lên tự động load driver, không cần Class.forName nữa
            Connection conn = DriverManager.getConnection(url, user, pass);

            assertNotNull(conn, "Kết nối thất bại!");
            System.out.println("Kết nối thành công tới database!");

            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}