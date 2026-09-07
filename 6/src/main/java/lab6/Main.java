package lab6;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.SQLException;

// Chạy thử: thăng chức nhân viên 10002 từ 'Staff' lên 'Senior Staff'
// giống Câu 2.1, dùng Java Transaction Management.
// Cấu hình kết nối: db.url / db.user / db.password. Tham số tuỳ chọn: emp_no.
public class Main {

    public static void main(String[] args) {
        int empNo = args.length > 0 ? Integer.parseInt(args[0]) : 10002;

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(getProp("db.url", "jdbc:mysql://localhost:3306/employees"));
        config.setUsername(getProp("db.user", "root"));
        config.setPassword(getProp("db.password", ""));
        config.setMaximumPoolSize(5);

        try (HikariDataSource dataSource = new HikariDataSource(config)) {
            TitlePromotionService service = new TitlePromotionService(dataSource);
            String outcome = service.promoteStaffToSenior(empNo);
            System.out.println("Da thang chuc: " + outcome);
        } catch (SQLException e) {
            System.err.println("LOI: " + e.getMessage() + " (da rollback, khong co thay doi nao)");
        }
    }

    private static String getProp(String key, String defaultValue) {
        String value = System.getProperty(key);
        return (value == null || value.isBlank()) ? defaultValue : value;
    }
}