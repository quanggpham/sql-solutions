package lab8;

// Dữ liệu PUT /employees/transfer. "title" là tùy chọn: nếu bỏ trống thì
// giữ nguyên chức danh hiện tại, chỉ chuyển phòng ban.
public record TransferRequest(int empNo, String deptNo, String title) {
}