start transaction;

-- select *
-- from titles t
-- where t.emp_no = 10002
-- 	and to_date = '9999-01-01'
-- for update;

update titles
set to_date = curdate()
where emp_no = 10002
	and title = 'Staff'
    and to_date = '9999-01-01';
    
insert into titles(emp_no, title, from_date, to_date)
values (10002, 'Senior Staff', curdate(), '9999-01-01');

commit;
