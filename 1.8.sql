-- 1.8.	Liệt kê các nhân viên nam chỉ làm một trong 2 phòng d003 và d002 (không liệt kê nhân viên đã từng làm cả 2 phòng)
select e.emp_no, e.gender
from employees e
join dept_emp de
	on e.emp_no = de.emp_no
where e.gender = 'M'
	and de.dept_no in ('d002', 'd003')
group by e.emp_no, e.gender
having count(distinct de.dept_no) = 1