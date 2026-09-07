package lab7;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// GET /employees — cả 4 query parameter đều tuỳ chọn:
//   hire_date_from : hire_date >= hire_date_from
//   salary         : lương hiện tại >= salary
//   dept_no        : đang trực thuộc phòng ban này
//   title          : chức danh hiện tại
@RestController
@RequestMapping("/employees")
public class EmployeeController {

    private final EmployeeRepository repository;

    public EmployeeController(EmployeeRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<Employee> find(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hireDateFrom,
            @RequestParam(required = false) BigDecimal salary,
            @RequestParam(required = false) String deptNo,
            @RequestParam(required = false) String title) {
        return repository.findBy(hireDateFrom, salary, deptNo, title);
    }
}