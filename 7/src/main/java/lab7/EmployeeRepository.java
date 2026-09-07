package lab7;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

// Truy vấn nhân viên với 4 bộ lọc tuỳ chọn. "Hiện tại" được nhận diện qua
// sentinel to_date = '9999-01-01' ở dept_emp / titles / salaries.
@Repository
public class EmployeeRepository {

    private final JdbcTemplate jdbc;

    public EmployeeRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Employee> findBy(LocalDate hireDateFrom, BigDecimal minSalary, String deptNo, String title) {
        Query query = buildQuery(hireDateFrom, minSalary, deptNo, title);
        return jdbc.query(query.sql(), EmployeeRepository::mapRow, query.params());
    }

    static Query buildQuery(LocalDate hireDateFrom, BigDecimal minSalary, String deptNo, String title) {
        StringBuilder sql = new StringBuilder("""
                SELECT e.emp_no, e.first_name, e.last_name, e.hire_date, e.gender
                FROM employees e
                JOIN dept_emp de ON e.emp_no = de.emp_no AND de.to_date = '9999-01-01'
                JOIN titles  t  ON e.emp_no = t.emp_no  AND t.to_date = '9999-01-01'
                JOIN salaries s ON e.emp_no = s.emp_no  AND s.to_date = '9999-01-01'
                WHERE 1 = 1
                """);
        List<String> clauses = new ArrayList<>();
        List<Object> params = new ArrayList<>();
        if (hireDateFrom != null) {
            clauses.add("e.hire_date >= ?");
            params.add(hireDateFrom);
        }
        if (minSalary != null) {
            clauses.add("s.salary >= ?");
            params.add(minSalary);
        }
        if (deptNo != null && !deptNo.isBlank()) {
            clauses.add("de.dept_no = ?");
            params.add(deptNo);
        }
        if (title != null && !title.isBlank()) {
            clauses.add("t.title = ?");
            params.add(title);
        }
        clauses.forEach(c -> sql.append(" AND ").append(c));
        return new Query(sql.toString(), params.toArray());
    }

    private static Employee mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new Employee(
                rs.getInt("emp_no"),
                rs.getString("first_name"),
                rs.getString("last_name"),
                rs.getDate("hire_date").toLocalDate(),
                rs.getString("gender"));
    }

    record Query(String sql, Object[] params) {
    }
}