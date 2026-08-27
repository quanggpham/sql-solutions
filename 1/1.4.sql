-- 1.4.	Tìm xem người quản lý có tên là Margareta Markovitch trong thời gian giữ chức quản lý thì đã quản lý bao nhiêu nhân viên.
-- (Đếm cả những nhân viên mà người này quản lý dù chỉ 1 ngày)

select count(distinct de.emp_no)
from dept_manager dm
join employees e
	on dm.emp_no = e.emp_no
	and e.first_name = 'Margareta' and e.last_name = 'Markovitch'
join dept_emp de
	on de.dept_no = dm.dept_no
    and de.from_date <= dm.to_date
    and de.to_date >= dm.from_date
    and de.emp_no != dm.emp_no 