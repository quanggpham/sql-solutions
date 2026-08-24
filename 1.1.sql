-- 1.1.	Liệt kê nhân viên bắt đầu làm việc từ năm 1995 nhận lương thấp nhất mỗi phòng (kỳ lương 25/07/1996).
select d.dept_no,
    d.dept_name,
    e.emp_no,
    e.first_name,
    e.last_name,
    s.salary
from employees e
join dept_emp de
	on e.emp_no = de.emp_no
	and de.from_date <= '1996-07-25'  and de.to_date >= '1996-07-25'
join departments d
	on d.dept_no = de.dept_no
join salaries s
	on e.emp_no = s.emp_no
	and s.from_date <= '1996-07-25' and  s.to_date >=  '1996-07-25'
join (
	select de2.dept_no, min(s2.salary) as min_salary
	from employees e2
    join dept_emp de2
		on e2.emp_no = de2.emp_no
		and de2.from_date <= '1996-07-25'  and de2.to_date >= '1996-07-25'
	join salaries s2
		on e2.emp_no = s2.emp_no
		and '1996-07-25' between s2.from_date and s2.to_date
	where e2.hire_date >= '1995-01-01'
	group by de2.dept_no
) min_table
	on min_table.dept_no = de.dept_no
    and min_table.min_salary = s.salary
where e.hire_date >= '1995-01-01'




-- AI
WITH TargetEmployees AS (
    -- 1. Lọc trước nhân viên thỏa mãn điều kiện năm để thu hẹp tập dữ liệu ngay từ đầu
    SELECT 
        e.emp_no,
        e.first_name,
        e.last_name
    FROM employees e
    WHERE e.hire_date >= '1995-01-01'
),
ActiveDeptSalary AS (
    -- 2. Kết hợp với phòng ban và lương tại thời điểm 25/07/1996, đánh số thứ tự phân vùng
    SELECT 
        d.dept_no,
        d.dept_name,
        te.emp_no,
        te.first_name,
        te.last_name,
        s.salary,
        DENSE_RANK() OVER (
            PARTITION BY de.dept_no 
            ORDER BY s.salary ASC
        ) AS salary_rank
    FROM TargetEmployees te
    INNER JOIN dept_emp de 
        ON te.emp_no = de.emp_no
        AND de.from_date <= '1996-07-25' 
        AND de.to_date   >= '1996-07-25'
    INNER JOIN departments d 
        ON de.dept_no = d.dept_no
    INNER JOIN salaries s 
        ON te.emp_no = s.emp_no
        AND s.from_date <= '1996-07-25' 
        AND s.to_date   >= '1996-07-25'
)
-- 3. Chỉ lấy những người có lương thấp nhất (rank = 1)
SELECT 
    dept_no,
    dept_name,
    emp_no,
    first_name,
    last_name,
    salary
FROM ActiveDeptSalary
WHERE salary_rank = 1
ORDER BY dept_no ASC;
