package com.mmo.shared.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DatabaseSeeder implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseSeeder(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        fixShopStatusTriggerAndUserRoles();
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
}
