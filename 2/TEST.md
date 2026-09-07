# TEST — Câu 2 (transactions, có MUTATE dữ liệu)

## Chuẩn bị
- Chạy trên **bản sao dùng thử** của CSDL `employees`, không phải bản gốc (mọi script đều UPDATE/DELETE/INSERT).
- Muốn chạy lại đúng trạng thái đầu → reload lại bộ dữ liệu (datacharmer/test_db).
- Chạy từ repo root: `cmd /c "mysql -u root -p employees < 2/2.1.sql"` (PowerShell không hỗ trợ `<`).

## 2.1 — Thăng chức 10002 Staff → Senior Staff
Chạy `2/2.1.sql`, rồi kiểm tra:

```sql
SELECT emp_no, title, from_date, to_date FROM titles WHERE emp_no = 10002 ORDER BY from_date;
```

Kỳ vọng:
- dòng cũ `'Staff'` giờ có `to_date = CURDATE()` (đóng chức vụ).
- có thêm dòng `'Senior Staff'` với `from_date = CURDATE()`, `to_date = '9999-01-01'`.

## 2.2 — Xóa phòng ban Production
Chạy `2/2.2.sql`, rồi kiểm tra:

```sql
SELECT COUNT(*) FROM departments WHERE dept_name = 'Production';                          -- 0
SELECT COUNT(*) FROM dept_emp de
JOIN departments d ON de.dept_no = d.dept_no WHERE d.dept_name = 'Production';            -- 0
SELECT COUNT(*) FROM employees e
JOIN dept_emp de ON e.emp_no = de.emp_no WHERE de.dept_no = 'd009';                       -- 0
```

Kỳ vọng: phòng `d009` không còn; mọi nhân viên từng thuộc Production đã bị xóa khỏi toàn bộ bảng liên quan (`salaries`, `titles`, `dept_emp`, `dept_manager`, `employees`). Kiểm tra thêm vài `emp_no` cũ của d009 ở từng bảng nếu cần.

## 2.3 — Tạo phòng Bigdata & ML + bổ nhiệm manager 10173
Chạy `2/2.3.sql`, rồi kiểm tra:

```sql
SELECT * FROM departments WHERE dept_no = 'd010';                              -- 'Bigdata & ML'
SELECT * FROM dept_manager WHERE emp_no = 10173 AND dept_no = 'd010';          -- 1 dòng đang mở
SELECT emp_no, dept_no, from_date, to_date FROM dept_emp WHERE emp_no = 10173 ORDER BY from_date;   -- dòng cũ đóng, dòng d010 mở
SELECT emp_no, title,     from_date, to_date FROM titles  WHERE emp_no = 10173 ORDER BY from_date;   -- dòng cũ đóng, 'Manager' mở
```

Kỳ vọng: 10173 hiện là `Manager` ở `d010`, dòng `dept_emp`/`titles` cũ đã đóng `to_date = CURDATE()`.