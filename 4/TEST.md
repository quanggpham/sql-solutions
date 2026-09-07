# TEST — Câu 4 (stored procedure `proc_2`, có MUTATE dữ liệu)

## Chuẩn bị
- Chạy trên **bản sao dùng thử** của `employees` (proc thuyên chuyển thật sự: UPDATE/INSERT).
- Cần `mysql` CLI (`delimiter //`).

## Bước 0 — Tạo proc

```
cmd /c "mysql -u root -p employees < 4/4.1.sql"
```

Script cuối có `call proc_2(...)` mẫu — nếu không muốn mutate ngay, comment dòng đó đi rồi chạy. (Nếu đã tạo bằng DBeaver wizard thì bỏ qua bước này.)

## Bước 1 — Trạng thái trước (10002 lúc gốc: d005/Development, title Staff)

```sql
SELECT e.emp_no, de.dept_no cur_dept, t.title cur_title
FROM employees e
JOIN dept_emp de ON e.emp_no = de.emp_no AND de.to_date = '9999-01-01'
JOIN titles t   ON e.emp_no = t.emp_no  AND t.to_date = '9999-01-01'
WHERE e.emp_no = 10002;
```

## Bước 2 — Happy path

```sql
call proc_2(10002, 'd004', 'Senior Engineer');
```

Result set trả về: `10002, Parto Bamford, M, Senior Engineer, Finance`.

## Bước 3 — Lịch sử sau khi chạy

```sql
SELECT 'titles'    tbl, emp_no, title   val, from_date, to_date FROM titles      WHERE emp_no = 10002
UNION ALL
SELECT 'dept_emp', emp_no, dept_no        , from_date, to_date FROM dept_emp    WHERE emp_no = 10002
UNION ALL
SELECT 'dept_mgr', emp_no, dept_no        , from_date, to_date FROM dept_manager WHERE emp_no = 10002
ORDER BY tbl, from_date;
```

Kỳ vọng: dòng cũ `to_date = CURDATE()`, dòng mới `to_date = '9999-01-01'`, không có `dept_mgr`.

## Bước 4 — Các nhánh lỗi (dữ liệu phải không đổi)

```sql
call proc_2(10002, 'd007', 'Manager');           -- ERROR: không bổ nhiệm Manager
call proc_2(999999, 'd004', 'Senior Engineer');  -- ERROR: nhân viên không tồn tại
call proc_2(10002, 'd099', 'Senior Engineer');   -- ERROR: phòng ban không tồn tại
call proc_2(10002, 'd004', 'Senior Engineer');   -- ERROR: đang ở d004 rồi
```

Kiểm chứng rollback (vẫn đúng 1 dòng mở):

```sql
SELECT (SELECT COUNT(*) FROM titles   WHERE emp_no = 10002 AND to_date = '9999-01-01') open_titles,
       (SELECT COUNT(*) FROM dept_emp WHERE emp_no = 10002 AND to_date = '9999-01-01') open_depts;
-- Kỳ vọng: 1 | 1
```

## Bước 5 — Đóng `dept_manager` khi thuyên chuyển (emp 110022 đang là manager d001)

```sql
SELECT emp_no, dept_no, from_date, to_date FROM dept_manager WHERE emp_no = 110022;  -- dòng d001 mở
call proc_2(110022, 'd002', 'Senior Engineer');
SELECT emp_no, dept_no, from_date, to_date FROM dept_manager WHERE emp_no = 110022;  -- d001 đóng, không có dòng mới
```

## Lưu ý schema
`dept_emp` PK = `(emp_no, dept_no)` → không cho nhân viên **quay lại phòng đã từng làm**. Sau vài lần thuyên chuyển, các lần chạy sau có thể dính `Duplicate entry ... for key 'dept_emp.PRIMARY'` → reload DB pristine rồi chạy lại.