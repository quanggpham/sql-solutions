package lab5;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.sql.DataSource;

// Gọi stored procedure proc_2 (câu 4) để thuyên chuyển nhân viên sang phòng ban
// và chức danh mới. Connection lấy từ pool và tự động trả về pool khi đóng
// (try-with-resources); Statement / ResultSet cũng được giải phóng trong khối
// try-with-resources như yêu cầu câu 5.
public class EmployeeTransferService {

    private final DataSource dataSource;

    public EmployeeTransferService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public TransferResult transfer(int empNo, String newDeptNo, String newTitle) {
        verifyCurrentEmployee(empNo);
        return callTransferProcedure(empNo, newDeptNo, newTitle);
    }

    // Dùng PreparedStatement để kiểm tra nhân viên đang làm việc trước khi thuyên chuyển
    private void verifyCurrentEmployee(int empNo) {
        String sql = """
                SELECT e.first_name, e.last_name
                FROM employees e
                JOIN dept_emp de ON e.emp_no = de.emp_no AND de.to_date = '9999-01-01'
                JOIN titles t ON e.emp_no = t.emp_no AND t.to_date = '9999-01-01'
                WHERE e.emp_no = ?
                """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, empNo);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("Nhân viên " + empNo + " không tồn tại hoặc không đang làm việc");
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Kiểm tra nhân viên thất bại", e);
        }
    }

    // Gọi stored procedure proc_2 bằng CallableStatement và đọc result set trả về
    private TransferResult callTransferProcedure(int empNo, String newDeptNo, String newTitle) {
        String sql = "{call proc_2(?, ?, ?)}";
        try (Connection conn = dataSource.getConnection();
             CallableStatement cs = conn.prepareCall(sql)) {
            cs.setInt(1, empNo);
            cs.setString(2, newDeptNo);
            cs.setString(3, newTitle);
            try (ResultSet rs = cs.executeQuery()) {
                if (rs.next()) {
                    return new TransferResult(
                            rs.getInt("emp_no"),
                            rs.getString("full_name"),
                            rs.getString("gender"),
                            rs.getString("title"),
                            rs.getString("dept_name"));
                }
                throw new SQLException("Không nhận được kết quả từ proc_2");
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Thuyên chuyển thất bại: " + e.getMessage(), e);
        }
    }
}