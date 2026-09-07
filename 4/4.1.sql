-- 4.1	Stored Procedure thuyên chuyển nhân viên sang phòng ban mới với chức danh mới.
-- (Không áp dụng bổ nhiệm làm Quản lý/Manager.)

delimiter //

drop procedure if exists proc_2 //

create procedure proc_2 (
    in p_emp_no int,
    in p_dept_no char(4),
    in p_new_title varchar(50)
)
begin
    declare v_current_dept char(4);

    declare exit handler for sqlexception
    begin
        rollback;
        resignal;
    end;

    start transaction;

    -- Kiểm tra nhân viên và phòng ban mới tồn tại
    if not exists (select 1 from employees where emp_no = p_emp_no) then
        signal sqlstate '45000' set message_text = 'Nhân viên không tồn tại';
    end if;

    if not exists (select 1 from departments where dept_no = p_dept_no) then
        signal sqlstate '45000' set message_text = 'Phòng ban mới không tồn tại';
    end if;

    -- Spec loại trừ việc bổ nhiệm Manager
    if lower(p_new_title) = 'manager' then
        signal sqlstate '45000' set message_text = 'Không áp dụng bổ nhiệm Manager qua SP thuyên chuyển';
    end if;

    -- Lấy phòng ban hiện tại của nhân viên
    select de.dept_no into v_current_dept
    from dept_emp de
    where de.emp_no = p_emp_no
        and de.to_date = '9999-01-01';

    if v_current_dept is null then
        signal sqlstate '45000' set message_text = 'Nhân viên không đang làm việc tại phòng ban nào';
    end if;

    -- Không thể "chuyển" sang đúng phòng ban hiện tại
    if v_current_dept = p_dept_no then
        signal sqlstate '45000' set message_text = 'Nhân viên đã làm việc tại phòng ban này';
    end if;

    -- Đóng chức vụ và phòng ban hiện tại
    update titles
    set to_date = curdate()
    where emp_no = p_emp_no and to_date = '9999-01-01';

    update dept_emp
    set to_date = curdate()
    where emp_no = p_emp_no and to_date = '9999-01-01';

    -- Nếu nhân viên đang là manager thì kết thúc vai trò quản lý
    update dept_manager
    set to_date = curdate()
    where emp_no = p_emp_no and to_date = '9999-01-01';

    -- Ghi nhận giai đoạn mới ở phòng ban và chức vụ mới
    insert into dept_emp (emp_no, dept_no, from_date, to_date)
    values (p_emp_no, p_dept_no, curdate(), '9999-01-01');

    insert into titles (emp_no, title, from_date, to_date)
    values (p_emp_no, p_new_title, curdate(), '9999-01-01');

    commit;

    -- Result set: thông tin nhân viên sau khi thuyên chuyển
    select e.emp_no, concat(e.first_name, ' ', e.last_name) as full_name, e.gender,
        t.title, d.dept_name
    from employees e
    join titles t
        on e.emp_no = t.emp_no
        and t.to_date = '9999-01-01'
    join dept_emp de
        on e.emp_no = de.emp_no
        and de.to_date = '9999-01-01'
    join departments d
        on de.dept_no = d.dept_no
    where e.emp_no = p_emp_no;
end //

delimiter ;

call proc_2(10002, 'd004', 'Senior Engineer');