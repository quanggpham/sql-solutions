start transaction;

insert into departments (dept_no, dept_name)
values ('d010', 'Bigdata & ML');

update dept_emp
set to_date = curdate()
where emp_no = 10173 and to_date = '9999-01-01';

update titles
set to_date = curdate()
where emp_no = 10173 and to_date = '9999-01-01';

insert into dept_emp (emp_no, dept_no, from_date, to_date)
values (10173, 'd010', curdate(), '9999-01-01');

insert into dept_manager (emp_no, dept_no, from_date, to_date)
values (10173, 'd010', CURDATE(), '9999-01-01');

insert into titles (emp_no, title, from_date, to_date)
values (10173, 'Manager', curdate(), '9999-01-01');

commit;