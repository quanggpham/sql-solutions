with cur_employees as (select e.emp_no, e.gender as gender, e.hire_date, de.dept_no as dept_no, t.title as title, s.salary as salary,
	timestampdiff(year, e.hire_date, curdate()) as exp_year
	
from employees e
join salaries s
	on e.emp_no = s.emp_no
    and s.to_date = '9999-01-01'
join dept_emp de
	on e.emp_no = de.emp_no
    and de.to_date = '9999-01-01'
join titles t
	on e.emp_no = t.emp_no
    and t.to_date = '9999-01-01'
)



select c.dept_no, c.title, c.exp_year,
	coalesce(avg(case when c.gender = 'M' then salary end), 0) as avg_male_salary,
    coalesce(avg(case when c.gender = 'F' then salary end), 0) as avg_female_salary,
    (avg(case when c.gender = 'M' then salary end) - avg(case when c.gender = 'F' then salary end)) as avg_gap
from cur_employees c
group by c.dept_no, c.title, c.exp_year