-- 1.3.	Lấy ra các nội dung sau của nhân viên có id = 10005: first_name, last_name, hire_date, salary_total.
-- Trong đó salary_total là tổng lương của nhân viên 10005 trong toàn bộ thời gian anh ta giữ chức vụ “Staff” – trong bảng titles.
select e.emp_no, e.first_name, e.last_name, e.hire_date, sum(
	s.salary * (
		period_diff(
			extract(year_month from(least(s.to_date, t.to_date) - interval 25 day)),
            extract(year_month from(greatest(s.from_date, t.from_date) - interval 25 day))
        ) + 1
    )
) as salary_total
from employees e
join titles t
	on e.emp_no = t.emp_no
join salaries s
	on s.emp_no = e.emp_no
    and s.from_date <= t.to_date and s.to_date >= t.from_date
where e.emp_no = 10005
	and t.title = 'Staff'
group by e.emp_no, e.first_name, e.last_name, e.hire_date