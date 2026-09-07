package lab8;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class EmployeeServiceTest {

    @Test
    void tachHoVaTen() {
        assertArrayEquals(new String[] {"Nguyễn", "Văn An"}, EmployeeService.splitName("Nguyễn Văn An"));
    }

    @Test
    void tenMotTuThiLastRong() {
        assertArrayEquals(new String[] {"An", ""}, EmployeeService.splitName("An"));
    }

    @Test
    void khoangTrangThuaDuocXoa() {
        assertArrayEquals(new String[] {"A", "B C"}, EmployeeService.splitName("  A   B C  "));
    }

    @Test
    void tongDiemTrongSo() {
        assertEquals(1.0, AnalyticsService.combinedScore(1.0, 1.0, 1.0, 1.0), 1e-12);
    }

    @Test
    void chiTietMoiTieuChi() {
        assertEquals(0.25, AnalyticsService.combinedScore(1.0, 0.0, 0.0, 0.0), 1e-12);
        assertEquals(0.30, AnalyticsService.combinedScore(0.0, 1.0, 0.0, 0.0), 1e-12);
        assertEquals(0.20, AnalyticsService.combinedScore(0.0, 0.0, 1.0, 0.0), 1e-12);
        assertEquals(0.25, AnalyticsService.combinedScore(0.0, 0.0, 0.0, 1.0), 1e-12);
    }
}