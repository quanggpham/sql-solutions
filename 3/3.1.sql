

delimiter //

drop procedure if exists proc_1 //
create procedure proc_1 (in p_name varchar(20)) 
begin
	select e.emp_no, concat(e.first_name, ' ', e.last_name) as full_name, e.gender, t.title, d.dept_name
	from employees e
    join titles t
		on e.emp_no = t.emp_no
        and t.to_date = '9999-01-01'
	join dept_emp de
		on e.emp_no = de.emp_no
        and de.to_date = '9999-01-01'
	join departments d
		on de.dept_no = d.dept_no
	where e.last_name = p_name or e.first_name = p_name;
    
with periods as (
select
	s.emp_no,
	s.from_date ,
	s.salary as salary,
	case
		when day(s.from_date) <= 25 then
		str_to_date(concat(date_format(s.from_date, '%Y-%m'), '-25'), '%Y-%m-%d')
		else
        str_to_date(concat(date_format(date_add(s.from_date, interval 1 month), '%Y-%m'), '-25'), '%Y-%m-%d')
	end as first_pay_date,
	case
		when day(if(s.to_date = '9999-01-01', curdate(), s.to_date)) >= 25 then
		str_to_date(concat(date_format(if(s.to_date = '9999-01-01', CURDATE(), s.to_date), '%Y-%m'), '-25'), '%Y-%m-%d')
		else
        str_to_date(concat(date_format(date_sub(if(s.to_date = '9999-01-01', CURDATE(), s.to_date), interval 1 month), '%Y-%m'), '-25'), '%Y-%m-%d')
	end as last_pay_date
from
	salaries s
join employees e on s.emp_no = e.emp_no 
	where e.last_name = p_name or e.first_name = p_name
),

monthly_breakdown as (
select
	emp_no,
	from_date,
	salary,
	case
		when
		first_pay_date <= last_pay_date then
		timestampdiff(month, first_pay_date, last_pay_date) + 1
		else
		0
	end
		as pay_months
from
		periods
)

select e.emp_no, concat(e.first_name , ' ', e.last_name ) as full_name,
sum(mb.salary * pay_months) as total_salary
from employees e 
join monthly_breakdown mb
	on mb.emp_no = e.emp_no

group by e.emp_no , e.first_name ,e.last_name ;

end //


delimiter ;

call proc_1('Facello')