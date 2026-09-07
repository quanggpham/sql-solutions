package lab8;

import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Tác vụ nền: 23:59 ngày 25 hàng tháng tính thuế TNCN theo công thức Câu 1.6
// cho toàn bộ nhân viên và ghi/lấy lại vào bảng income_tax.
// Bảng income_tax được tự động tạo nếu chưa tồn tại.
@Service
public class IncomeTaxJob {

    private final JdbcTemplate jdbc;

    public IncomeTaxJob(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
        jdbc.execute("""
                CREATE TABLE IF NOT EXISTS income_tax (
                    emp_no INT NOT NULL,
                    tax INT NOT NULL,
                    month TINYINT NOT NULL,
                    year SMALLINT NOT NULL,
                    PRIMARY KEY (emp_no, month, year)
                )
                """);
    }

    @Scheduled(cron = "0 59 23 25 * ?")
    @Transactional
    public void run() {
        LocalDate now = LocalDate.now();
        runFor(now.getYear(), now.getMonthValue());
    }

    // Có thể gọi thủ công (POST /payroll/tax/run) để kiểm tra; khi tất toán
    // khóa FOR UPDATE bảng salaries ngăn mọi sửa đổi song song trong kỳ.
    @Transactional
    public TaxRunResult runFor(int year, int month) {
        if (month < 1 || month > 12 || year < 1900 || year > 2100) {
            throw new IllegalArgumentException("month hoặc year không hợp lệ");
        }
        LocalDate payday = LocalDate.of(year, month, 25);

        List<long[]> salaries = jdbc.query("""
                SELECT emp_no, salary FROM salaries
                WHERE from_date <= ? AND to_date >= ?
                FOR UPDATE
                """,
                (rs, i) -> new long[] {rs.getLong("emp_no"), rs.getLong("salary")}, payday, payday);

        long totalTax = 0;
        for (long[] s : salaries) {
            long tax = computeTax(s[1]);
            totalTax += tax;
            jdbc.update("""
                    INSERT INTO income_tax (emp_no, tax, month, year) VALUES (?, ?, ?, ?)
                    ON DUPLICATE KEY UPDATE tax = VALUES(tax)
                    """, s[0], tax, month, year);
        }
        return new TaxRunResult(year, month, salaries.size(), totalTax);
    }

    // Thuế TNCN lũy tiến theo Câu 1.6 (đơn vị tiền tệ của bảng salaries).
    static long computeTax(long salary) {
        if (salary <= 40000) {
            return 0;
        } else if (salary <= 60000) {
            return Math.round((salary - 40000) * 0.05);
        } else if (salary <= 90000) {
            return Math.round(20000 * 0.05 + (salary - 60000) * 0.10);
        } else {
            return Math.round(20000 * 0.05 + 30000 * 0.10 + (salary - 90000) * 0.15);
        }
    }

    public record TaxRunResult(int year, int month, long employees, long totalTax) {
    }
}