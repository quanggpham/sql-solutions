package lab8;

import java.math.BigDecimal;
import java.time.LocalDate;

// Nhân viên sau khi khởi tạo (POST /employees).
public record Employee(
        int empNo,
        String firstName,
        String lastName,
        LocalDate birthDate,
        String gender,
        LocalDate hireDate,
        String deptNo,
        String title,
        BigDecimal salary) {
}