-- 1.11	Trong mỗi phòng ban, hãy tìm 5 người đang làm việc giữ chức vụ staff có lương cao nhất.

with ranked as (
select de.emp_no, de.dept_no, s.salary,
	row_number() over(partition by de.dept_no order by s.salary desc) as ranking
from dept_emp de
join titles t
	on de.emp_no = t.emp_no
join salaries s
	on de.emp_no = s.emp_no
where de.to_date  = '9999-01-01'
	and t.title = 'Staff'
    and s.to_date  = '9999-01-01'
    and t.to_date  = '9999-01-01'
)

select *
from ranked
where ranking <= 5
