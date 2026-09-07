package lab7;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class EmployeeRepositoryTest {

    @Test
    void khongTruyenThamSoThiKhongCoDieuKienLoc() {
        EmployeeRepository.Query q = EmployeeRepository.buildQuery(null, null, null, null);
        assertEquals(0, q.params().length);
        assertFalse(q.sql().contains("?"));
    }

    @Test
    void truyenThamSoThiSinhRaDieuKienTuongUng() {
        EmployeeRepository.Query q = EmployeeRepository.buildQuery(
                LocalDate.of(1995, 1, 1),
                new BigDecimal("50000.00"),
                "d005",
                "Staff");
        assertEquals(4, q.params().length);
        assertArrayEquals(
                new Object[] {
                    LocalDate.of(1995, 1, 1), new BigDecimal("50000.00"), "d005", "Staff"
                },
                q.params());
        assertTrue(q.sql().contains("e.hire_date >= ?"));
        assertTrue(q.sql().contains("s.salary >= ?"));
        assertTrue(q.sql().contains("de.dept_no = ?"));
        assertTrue(q.sql().contains("t.title = ?"));
    }

    @Test
    void thamSoTrongKhongDuocThemDieuKien() {
        EmployeeRepository.Query q = EmployeeRepository.buildQuery(null, null, "", "  ");
        assertEquals(0, q.params().length);
    }
}