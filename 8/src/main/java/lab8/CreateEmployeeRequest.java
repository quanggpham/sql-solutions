package lab8;

import java.math.BigDecimal;
import java.time.LocalDate;

// Dữ liệu POST /employees. "name" là Họ + Tên đầy đủ, backend tự tách.
public record CreateEmployeeRequest(
        LocalDate birthDate,
        String name,
        String gender,
        LocalDate hireDate,
        String title,
        BigDecimal salary,
        String deptNo) {
}