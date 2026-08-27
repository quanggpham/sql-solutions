-- 1.2.	Hãy tìm ra phòng nào có mức lương trung bình cao nhất. 
select d.dept_no, d.dept_name, avg(s.salary) as avg_salary
from departments d
join dept_emp de
	on d.dept_no = de.dept_no
    and de.to_date = '9999-01-01'
join salaries s
	on de.emp_no = s.emp_no
    and s.to_date = '9999-01-01'
group by dept_no, dept_name
order by avg_salary desc
limit 1




-- AI
WITH DepartmentAvgSalary AS (
    -- Tầng 1: Tính toán mức lương trung bình hiện tại của từng phòng
    SELECT 
        d.dept_no,
        d.dept_name,
        AVG(s.salary) AS avg_salary
    FROM departments d
    INNER JOIN dept_emp de 
        ON d.dept_no = de.dept_no
        AND de.to_date = '9999-01-01'
    INNER JOIN salaries s 
        ON de.emp_no = s.emp_no
        AND s.to_date = '9999-01-01'
    GROUP BY 
        d.dept_no, 
        d.dept_name
),
RankedDepartments AS (
    -- Tầng 2: Xếp hạng phòng ban theo lương trung bình giảm dần
    SELECT 
        dept_no,
        dept_name,
        avg_salary,
        DENSE_RANK() OVER (ORDER BY avg_salary DESC) AS ranking
    FROM DepartmentAvgSalary
)
-- Tầng 3: Lấy toàn bộ các phòng ban đạt Top 1 (bảo toàn nếu có đồng hạng)
SELECT 
    dept_no,
    dept_name,
    ROUND(avg_salary, 2) AS avg_salary
FROM RankedDepartments
WHERE ranking = 1;

