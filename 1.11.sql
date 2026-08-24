-- 1.11	Trong mỗi phòng ban, hãy tìm 5 người đang làm việc giữ chức vụ staff có lương cao nhất.
select *
from dept_emp de
join titles t
	on t.emp_no = de.emp_no
    and t.title = 'Staff'
    and t.to_date = '9999-01-01'
where de.to_date = '9999-01-01'