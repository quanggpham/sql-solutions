-- 1.7.	Tính lương phải trả cho từng phòng trong năm 1996.
select de.dept_no, d.dept_name, sum(
	s.salary * (
    period_diff(
				extract(year_month from( least(de.to_date, s.to_date, '1996-12-31') - interval 25 day)),
				extract(year_month from (greatest(de.from_date, s.from_date, '1996-01-01') - interval 25 day))
                )+ 1)
) as total_salary
from salaries s
join dept_emp de
	on s.emp_no = de.emp_no
join departments d
	on de.dept_no = d.dept_no
where s.from_date <= '1996-12-31' and s.to_date >= '1996-01-01'
	and de.from_date <= '1996-12-31' and de.to_date >= '1996-01-01'
    and de.from_date <= s.to_date and de.to_date >= s.from_date
group by de.dept_no, d.dept_name