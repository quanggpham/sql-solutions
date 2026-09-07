package lab6;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.sql.DataSource;

// Thực thi chuỗi thao tác của CÂU 2.1 bằng Java Transaction Management:
// 1) Đóng/dừng chức vụ "Staff" hiện tại (set to_date = CURDATE()).
// 2) Tạo bản ghi chức vụ mới "Senior Staff" (insert, to_date = '9999-01-01').
//
// Tắt auto-commit; nếu bất kỳ ngoại lệ nào xảy ra thì rollback() toàn bộ,
// chỉ commit() khi tất cả thao tác thành công. Connection lấy từ pool và được
// trả về pool qua try-with-resources.
public class TitlePromotionService {

    private final DataSource dataSource;

    public TitlePromotionService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public String promoteStaffToSenior(int empNo) throws SQLException {
        String closeStaff = "UPDATE titles SET to_date = CURDATE() "
                + "WHERE emp_no = ? AND title = 'Staff' AND to_date = '9999-01-01'";
        String insertSenior = "INSERT INTO titles (emp_no, title, from_date, to_date) "
                + "VALUES (?, 'Senior Staff', CURDATE(), '9999-01-01')";
        String selectInfo = "SELECT e.emp_no, CONCAT(e.first_name, ' ', e.last_name) AS full_name, "
                + "t.title, t.from_date, t.to_date "
                + "FROM employees e JOIN titles t ON e.emp_no = t.emp_no "
                + "WHERE e.emp_no = ? AND t.title = 'Senior Staff' AND t.to_date = '9999-01-01'";

        try (Connection conn = dataSource.getConnection()) {
            // 1) Tắt auto-commit: các thao tác tiếp theo chưa được ghi nhận vĩnh viễn
            conn.setAutoCommit(false);

            String outcome;
            try {
                int closed = 0;
                try (PreparedStatement ps = conn.prepareStatement(closeStaff)) {
                    ps.setInt(1, empNo);
                    closed = ps.executeUpdate();
                }

                if (closed != 1) {
                    throw new SQLException("Nhân viên " + empNo + " không đang giữ chức vụ 'Staff'"
                            + (closed == 0 ? " (không có row nào bị đóng)" : " (đóng sai số row)"));
                }

                try (PreparedStatement ps = conn.prepareStatement(insertSenior)) {
                    ps.setInt(1, empNo);
                    ps.executeUpdate();
                }

                try (PreparedStatement ps = conn.prepareStatement(selectInfo)) {
                    ps.setInt(1, empNo);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            outcome = "emp_no=" + rs.getInt("emp_no")
                                    + ", full_name=" + rs.getString("full_name")
                                    + ", title=" + rs.getString("title")
                                    + ", from_date=" + rs.getDate("from_date")
                                    + ", to_date=" + rs.getDate("to_date");
                        } else {
                            throw new SQLException("Không đọc được kết quả sau khi thăng chức");
                        }
                    }
                }

                // 2b) Tất cả thao tác thành công mới commit
                conn.commit();
            } catch (SQLException e) {
                // 2a) Có ngoại lệ -> rollback toàn bộ, không để lại dữ liệu nửa vời
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }

            return outcome;
        }
    }
}