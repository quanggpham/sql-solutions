package lab8;

import java.time.LocalDate;
import java.util.List;

// Chi tiết bảng lương của một kỳ (lấy theo các bản ghi lương chứa ngày 25).
public record PayrollSummary(
        int year,
        int month,
        List<PayrollLine> employees,
        List<DepartmentTotal> departments,
        long salaryTotal,
        long taxTotal) {

    public record PayrollLine(
            int empNo,
            String fullName,
            String deptNo,
            String deptName,
            String title,
            long salary,
            Long tax) {
    }

    public record DepartmentTotal(
            String deptNo,
            String deptName,
            long salaryTotal,
            long taxTotal) {
    }
}