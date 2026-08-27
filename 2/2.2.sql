
SET SQL_SAFE_UPDATES = 0;

create temporary table tmp_emp_no as
select de.emp_no as emp_no
from dept_emp de
join departments d
	on de.dept_no = d.dept_no
where d.dept_name = 'Production';

alter table tmp_emp_no add primary key (emp_no);

start transaction;

delete s 
from salaries s 
join tmp_emp_no ten
	on s.emp_no = ten.emp_no;
    
delete t
from titles t
join tmp_emp_no ten
	on t.emp_no = ten.emp_no;
    
delete dm from dept_manager dm join tmp_emp_no t on dm.emp_no = t.emp_no;
delete de from dept_emp de join tmp_emp_no t on de.emp_no = t.emp_no;

delete e
from employees e
join tmp_emp_no ten
	on e.emp_no = ten.emp_no;

delete from departments where dept_name = 'Production';

commit;

SET SQL_SAFE_UPDATES = 1;

drop temporary table if exists tmp_emp_no;