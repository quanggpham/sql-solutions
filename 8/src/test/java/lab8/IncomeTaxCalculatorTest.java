package lab8;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class IncomeTaxCalculatorTest {

    @Test
    void khongTinhThueDuoi40k() {
        assertEquals(0, IncomeTaxJob.computeTax(40000));
        assertEquals(0, IncomeTaxJob.computeTax(39000));
    }

    @Test
    void tinh5PhanTramChoPhanVuot40k() {
        assertEquals(250, IncomeTaxJob.computeTax(45000));
        assertEquals(1000, IncomeTaxJob.computeTax(60000));
    }

    @Test
    void tinhThem10PhanTramChoPhanVuot60k() {
        assertEquals(3000, IncomeTaxJob.computeTax(80000));
        assertEquals(4000, IncomeTaxJob.computeTax(90000));
    }

    @Test
    void tinhThem15PhanTramChoPhanVuot90k() {
        assertEquals(5500, IncomeTaxJob.computeTax(100000));
        assertEquals(14500, IncomeTaxJob.computeTax(160000));
    }
}