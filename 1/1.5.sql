-- 1.5.	Tính lương trung bình phải trả cho từng nhóm title trong kỳ lương 25/07/1996.
select t.title, avg(s.salary) as agv_salary
from salaries s
join titles t
	on t.emp_no = s.emp_no
    and t.from_date <= '1996-07-25' and t.to_date >= '1996-07-25'
where s.from_date <= '1996-07-25' and s.to_date >= '1996-07-25'
group by t.title