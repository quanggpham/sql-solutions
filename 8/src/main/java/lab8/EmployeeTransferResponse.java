package lab8;

// Kết quả sau khi thuyên chuyển (giống result set của proc_2 câu 4).
public record EmployeeTransferResponse(
        int empNo,
        String fullName,
        String gender,
        String deptNo,
        String deptName,
        String title) {
}