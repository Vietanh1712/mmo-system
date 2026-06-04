package dal;

import java.sql.Connection;
import java.sql.DriverManager;

public class DBContext {
    public Connection getConnection() throws Exception {
        // Cấu hình kết nối
        String url = "jdbc:sqlserver://localhost:1433;databaseName=MMO_System;encrypt=true;trustServerCertificate=true";
        String user = "NNA";
        String pass = "123";

        // Load Driver (Thư viện này đã được Maven tải về nhờ file pom.xml của bạn)
        Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");

        return DriverManager.getConnection(url, user, pass);
    }
}