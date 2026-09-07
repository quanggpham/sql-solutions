package lab8;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class TitleCareerAnalyzeTest {

    private static LocalDate d(String s) {
        return LocalDate.parse(s);
    }

    // Nhân viên thăng tiến bình thường: không giáng chức.
    @Test
    void thangTienBinhThuongKhongBiGiangChuc() {
        List<AnalyticsService.TitleEvent> events = List.of(
                new AnalyticsService.TitleEvent(d("2000-01-01"), d("2001-01-01"), "Staff"),
                new AnalyticsService.TitleEvent(d("2001-01-01"), d("9999-01-01"), "Senior Staff"));
        AnalyticsService.TitleCareer a = AnalyticsService.analyze(events);
        assertEquals(1, a.changes());
        assertFalse(a.demoted());
        assertEquals(-1, a.recoveryDays());
    }

    // Bị giáng chức rồi vượt qua chức vụ khởi điểm: đếm 5 lần thay chức vụ,
    // 1 lần giáng chức, phục hồi sau 731 ngày.
    @Test
    void giangChucRoiPhucHoiTinhDungSoNgay() {
        List<AnalyticsService.TitleEvent> events = List.of(
                new AnalyticsService.TitleEvent(d("2000-01-01"), d("2001-01-01"), "Staff"),
                new AnalyticsService.TitleEvent(d("2001-01-01"), d("2002-01-01"), "Senior Staff"),
                new AnalyticsService.TitleEvent(d("2002-01-01"), d("2003-01-01"), "Staff"),
                new AnalyticsService.TitleEvent(d("2003-01-01"), d("2004-01-01"), "Senior Staff"),
                new AnalyticsService.TitleEvent(d("2004-01-01"), d("2005-01-01"), "Engineer"),
                new AnalyticsService.TitleEvent(d("2005-01-01"), d("9999-01-01"), "Senior Engineer"));
        AnalyticsService.TitleCareer a = AnalyticsService.analyze(events);
        assertEquals(5, a.changes());
        assertTrue(a.demoted());
        // Giáng chức ngày 2002-01-01, vượt qua "Staff" khi thành Senior Staff ngày 2003-01-01.
        assertEquals(365, a.recoveryDays());
        assertEquals("Senior Engineer", a.currentTitle());
    }

    // Giáng chức nhưng chưa từng vượt qua chức vụ khởi điểm -> recoveryDays = -1.
    @Test
    void giangChucNhungChuaPhucHoi() {
        List<AnalyticsService.TitleEvent> events = List.of(
                new AnalyticsService.TitleEvent(d("2000-01-01"), d("2001-01-01"), "Engineer"),
                new AnalyticsService.TitleEvent(d("2001-01-01"), d("9999-01-01"), "Staff"));
        AnalyticsService.TitleCareer a = AnalyticsService.analyze(events);
        assertTrue(a.demoted());
        assertEquals(-1, a.recoveryDays());
    }
}