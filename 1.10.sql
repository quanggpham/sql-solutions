-- 1.10	Trong bảng titles một người có thể giữ nhiều chức vụ khác nhau, nhưng chỉ có 1 chức vụ hiện tại là những title có to_date là ngày 01-01-9999
-- Liệt kê những emp_no đang làm việc mà giữ 2 chức vụ trở lên.

select t.emp_no, count(distinct t.title) as total_titles
from titles t
where t.emp_no in (
	select de.emp_no
    from dept_emp de
    where de.to_date = '9999-01-01'
)
group by t.emp_no
having total_titles >= 2