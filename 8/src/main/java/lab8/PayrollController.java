package lab8;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// Nhóm API bảng lương + tác vụ thuế.
@RestController
@RequestMapping("/payroll")
public class PayrollController {

    private final PayrollService payroll;
    private final IncomeTaxJob taxJob;

    public PayrollController(PayrollService payroll, IncomeTaxJob taxJob) {
        this.payroll = payroll;
        this.taxJob = taxJob;
    }

    @GetMapping("/summary")
    public PayrollSummary summary(@RequestParam int year, @RequestParam int month) {
        return payroll.summary(year, month);
    }

    // Chạy thủ công tác vụ thuế để kiểm tra (tác vụ nền tự chạy 23:59 ngày 25).
    @PostMapping("/tax/run")
    public IncomeTaxJob.TaxRunResult runTax(@RequestParam int year, @RequestParam int month) {
        return taxJob.runFor(year, month);
    }
}