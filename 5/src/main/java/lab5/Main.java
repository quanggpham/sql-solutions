package lab5;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

// Gọi proc_2 qua Connection Pool (HikariCP).
// Cấu hình kết nối từ system properties: db.url / db.user / db.password.
// Tham số dòng lệnh (tuỳ chọn): emp_no, dept_no mới, title mới.
public class Main {

    public static void main(String[] args) {
        int empNo = args.length > 0 ? Integer.parseInt(args[0]) : 10010;
        String newDeptNo = args.length > 1 ? args[1] : "d007";
        String newTitle = args.length > 2 ? args[2] : "Senior Engineer";

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(getProp("db.url", "jdbc:mysql://localhost:3306/employees"));
        config.setUsername(getProp("db.user", "root"));
        config.setPassword(getProp("db.password", ""));
        config.setMaximumPoolSize(5);

        // Try-with-resources: HikariDataSource (pool) được đóng khi kết thúc
        try (HikariDataSource dataSource = new HikariDataSource(config)) {
            EmployeeTransferService service = new EmployeeTransferService(dataSource);
            TransferResult result = service.transfer(empNo, newDeptNo, newTitle);
            System.out.println("Da thuyen chuyen (emp=" + empNo + ", dept=" + newDeptNo + ", title=" + newTitle + "): " + result);
        }
    }

    private static String getProp(String key, String defaultValue) {
        String value = System.getProperty(key);
        return (value == null || value.isBlank()) ? defaultValue : value;
    }
}