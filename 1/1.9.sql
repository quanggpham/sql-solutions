-- 1.9.	Tìm xem tổng lương phải trả của mỗi phòng ban trong kỳ lương 25/07/1996 là bao nhiêu và lọc những phòng ban trả tổng lương cao hơn 300k$.
select de.dept_no, d.dept_name, sum(s.salary) as total_salary
from salaries s
join dept_emp de
	on s.emp_no = de.emp_no
    and de.from_date <= '1996-07-25' and de.to_date >= '1996-07-25'
 --    and de.from_date <= s.to_date and de.to_date >= s.from_date
join departments d
	on de.dept_no = d.dept_no
where s.from_date <= '1996-07-25' and s.to_date >= '1996-07-25'
group by de.dept_no, d.dept_name
having total_salary > 3000000000
-- 3 tỉ đô 