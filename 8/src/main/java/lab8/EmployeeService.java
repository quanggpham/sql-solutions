package lab8;

import java.time.LocalDate;
import java.util.Arrays;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

// Nghiệp vụ nhân sự: tạo mới nhân viên (POST /employees) và thuyên chuyển
// (PUT /employees/transfer). Mọi thao tác ghi nhiều bảng được bọc transaction.
@Service
public class EmployeeService {

    static final String DEFAULT_DEPT = "d005";

    private final JdbcTemplate jdbc;

    public EmployeeService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public Employee create(CreateEmployeeRequest req) {
        if (req.name() == null || req.name().isBlank()) {
            throw new IllegalArgumentException("name không được rỗng");
        }
        if (req.gender() == null || !(req.gender().equalsIgnoreCase("M") || req.gender().equalsIgnoreCase("F"))) {
            throw new IllegalArgumentException("gender phải là 'M' hoặc 'F'");
        }
        if (req.title() == null || req.title().isBlank()) {
            throw new IllegalArgumentException("title không được rỗng");
        }
        if (req.salary() == null || req.salary().signum() <= 0) {
            throw new IllegalArgumentException("salary phải lớn hơn 0");
        }
        if (req.birthDate() == null || req.hireDate() == null) {
            throw new IllegalArgumentException("birthDate và hireDate là bắt buộc");
        }

        String[] parts = splitName(req.name());
        String deptNo = (req.deptNo() == null || req.deptNo().isBlank()) ? DEFAULT_DEPT : req.deptNo();
        int empNo = jdbc.queryForObject("SELECT COALESCE(MAX(emp_no), 0) + 1 FROM employees", Integer.class);
        LocalDate sentinel = LocalDate.of(9999, 1, 1);

        jdbc.update("INSERT INTO employees (emp_no, birth_date, first_name, last_name, gender, hire_date) VALUES (?, ?, ?, ?, ?, ?)",
                empNo, req.birthDate(), parts[0], parts[1], req.gender().toUpperCase(), req.hireDate());
        jdbc.update("INSERT INTO titles (emp_no, title, from_date, to_date) VALUES (?, ?, ?, ?)",
                empNo, req.title(), req.hireDate(), sentinel);
        jdbc.update("INSERT INTO salaries (emp_no, salary, from_date, to_date) VALUES (?, ?, ?, ?)",
                empNo, req.salary(), req.hireDate(), sentinel);
        jdbc.update("INSERT INTO dept_emp (emp_no, dept_no, from_date, to_date) VALUES (?, ?, ?, ?)",
                empNo, deptNo, req.hireDate(), sentinel);

        return new Employee(empNo, parts[0], parts[1], req.birthDate(), req.gender().toUpperCase(),
                req.hireDate(), deptNo, req.title(), req.salary());
    }

    @Transactional
    public EmployeeTransferResponse transfer(TransferRequest req) {
        String title = (req.title() == null || req.title().isBlank()) ? null : req.title().trim();
        if (title != null && title.equalsIgnoreCase("Manager")) {
            throw new IllegalArgumentException("Không áp dụng bổ nhiệm Manager qua API thuyên chuyển");
        }
        if (jdbc.queryForObject("SELECT COUNT(*) FROM departments WHERE dept_no = ?",
                Integer.class, req.deptNo()) == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Phòng ban không tồn tại");
        }

        String curDept = jdbc.query("SELECT de.dept_no FROM dept_emp de WHERE de.emp_no = ? AND de.to_date = '9999-01-01'",
                rs -> rs.next() ? rs.getString(1) : null, req.empNo());
        if (curDept == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Nhân viên không tồn tại hoặc không đang làm việc");
        }
        if (curDept.equals(req.deptNo())) {
            throw new IllegalArgumentException("Nhân viên đã làm việc tại phòng ban này");
        }

        LocalDate today = LocalDate.now();
        LocalDate sentinel = LocalDate.of(9999, 1, 1);

        jdbc.update("UPDATE dept_emp SET to_date = ? WHERE emp_no = ? AND to_date = '9999-01-01'", today, req.empNo());
        jdbc.update("UPDATE dept_manager SET to_date = ? WHERE emp_no = ? AND to_date = '9999-01-01'", today, req.empNo());
        if (title != null) {
            jdbc.update("UPDATE titles SET to_date = ? WHERE emp_no = ? AND to_date = '9999-01-01'", today, req.empNo());
            jdbc.update("INSERT INTO titles (emp_no, title, from_date, to_date) VALUES (?, ?, ?, ?)",
                    req.empNo(), title, today, sentinel);
        }
        jdbc.update("INSERT INTO dept_emp (emp_no, dept_no, from_date, to_date) VALUES (?, ?, ?, ?)",
                req.empNo(), req.deptNo(), today, sentinel);

        EmployeeTransferResponse result = jdbc.query("""
                SELECT e.emp_no, CONCAT(e.first_name, ' ', e.last_name) AS full_name, e.gender,
                       de.dept_no, d.dept_name, t.title
                FROM employees e
                JOIN dept_emp de ON de.emp_no = e.emp_no AND de.to_date = '9999-01-01'
                JOIN departments d ON d.dept_no = de.dept_no
                JOIN titles t ON t.emp_no = e.emp_no AND t.to_date = '9999-01-01'
                WHERE e.emp_no = ?
                """, rs -> rs.next()
                ? new EmployeeTransferResponse(rs.getInt("emp_no"), rs.getString("full_name"),
                        rs.getString("gender"), rs.getString("dept_no"), rs.getString("dept_name"),
                        rs.getString("title"))
                : null, req.empNo());
        if (result == null) {
            throw new IllegalStateException("Không đọc được kết quả sau khi thuyên chuyển");
        }
        return result;
    }

    // Tách "Họ và tên" -> token đầu là first_name, phần còn lại là last_name.
    static String[] splitName(String fullName) {
        String[] tokens = fullName.trim().split("\\s+");
        if (tokens.length == 1) {
            return new String[] {tokens[0], ""};
        }
        return new String[] {tokens[0], String.join(" ", Arrays.copyOfRange(tokens, 1, tokens.length))};
    }
}