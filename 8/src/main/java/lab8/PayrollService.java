package lab8;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

// Truy vấn bảng lương theo tháng/năm (GET /payroll/summary).
@Service
public class PayrollService {

    private final JdbcTemplate jdbc;

    public PayrollService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public PayrollSummary summary(int year, int month) {
        if (month < 1 || month > 12 || year < 1900 || year > 2100) {
            throw new IllegalArgumentException("month hoặc year không hợp lệ");
        }
        LocalDate payday = LocalDate.of(year, month, 25);

        List<PayrollSummary.PayrollLine> lines = jdbc.query("""
                SELECT e.emp_no,
                       CONCAT(e.first_name, ' ', e.last_name) AS full_name,
                       de.dept_no, d.dept_name, t.title, s.salary, it.tax
                FROM salaries s
                JOIN employees e ON e.emp_no = s.emp_no
                JOIN dept_emp de ON de.emp_no = e.emp_no AND de.to_date = '9999-01-01'
                JOIN departments d ON d.dept_no = de.dept_no
                JOIN titles t ON t.emp_no = e.emp_no AND t.to_date = '9999-01-01'
                LEFT JOIN income_tax it ON it.emp_no = e.emp_no AND it.month = ? AND it.year = ?
                WHERE s.from_date <= ? AND s.to_date >= ?
                ORDER BY de.dept_no, e.emp_no
                """,
                (rs, i) -> {
                    long tax = rs.getLong("tax");
                    return new PayrollSummary.PayrollLine(
                            rs.getInt("emp_no"), rs.getString("full_name"),
                            rs.getString("dept_no"), rs.getString("dept_name"), rs.getString("title"),
                            rs.getLong("salary"), rs.wasNull() ? null : tax);
                },
                month, year, payday, payday);

        // Gom tổng lương + thuế theo phòng ban.
        Map<String, PayrollSummary.DepartmentTotal> byDept = new LinkedHashMap<>();
        for (PayrollSummary.PayrollLine l : lines) {
            PayrollSummary.DepartmentTotal t = byDept.get(l.deptNo());
            long salary = t == null ? 0 : t.salaryTotal();
            long tax = t == null ? 0 : t.taxTotal();
            long lineTax = l.tax() == null ? 0 : l.tax();
            byDept.put(l.deptNo(), new PayrollSummary.DepartmentTotal(l.deptNo(), l.deptName(), salary + l.salary(), tax + lineTax));
        }
        List<PayrollSummary.DepartmentTotal> departments = new ArrayList<>(byDept.values());
        departments.sort(Comparator.comparing(PayrollSummary.DepartmentTotal::deptNo));

        long salaryTotal = lines.stream().mapToLong(PayrollSummary.PayrollLine::salary).sum();
        long taxTotal = lines.stream().mapToLong(l -> l.tax() == null ? 0 : l.tax()).sum();
        return new PayrollSummary(year, month, lines, departments, salaryTotal, taxTotal);
    }
}