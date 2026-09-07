package lab7;

import java.time.LocalDate;

// DTO trả về: thông tin nhân viên từ bảng employees.
public record Employee(
        int empNo,
        String firstName,
        String lastName,
        LocalDate hireDate,
        String gender) {
}