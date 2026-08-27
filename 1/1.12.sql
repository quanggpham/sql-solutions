-- 1.8	Tìm các nhân viên đã từng làm ở 2 phòng ban khác nhau, và phải giữ chức vụ khác nhau khi làm ở mỗi phòng ban.

with history as(
	select distinct de.emp_no as emp_no, de.dept_no as dept_no, t.title as title
	from dept_emp de
	join titles t
		on de.emp_no = t.emp_no
		and de.from_date <= t.to_date
		and de.to_date >= t.from_date
)

select distinct h1.emp_no, h2.dept_no, h1.dept_no
from history h1
join history h2
	on h1.emp_no = h2.emp_no
    and h1.dept_no < h2.dept_no
    and h1.title <> h2.title