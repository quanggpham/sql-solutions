# TEST — Câu 5 (Java gọi `proc_2` qua HikariCP)

## Chuẩn bị
- CSDL `employees` đang mở + **`proc_2` đã tồn tại** (chạy `4/4.1.sql` một lần, hoặc tạo bằng DBeaver).
- JDK 17+ và Maven.

## Chạy

```powershell
mvn -f 5/pom.xml compile exec:java "-Ddb.password=123456" "-Dexec.args=10010 d007 Senior Engineer"
```

- `-Ddb.url` / `-Ddb.user` / `-Ddb.password`: đổi nếu không phải mặc định (`localhost/3306`, user `root`).
- `-Dexec.args` = `emp_no dept_no title` — đổi 3 giá trị này để test nhân viên/phòng khác, không cần sửa code.
- **PowerShell bắt buộc nháy kép** `-D...`; trên `cmd` bỏ nháy.

## Verify happy path
1. Chọn emp + target dept còn "trong sạch" (target ≠ phòng hiện tại VÀ không nằm trong lịch sử):
```sql
SELECT de.emp_no, de.dept_no cur_dept, t.title cur_title
FROM dept_emp de JOIN titles t ON de.emp_no = t.emp_no
WHERE de.emp_no = 10010 AND de.to_date = '9999-01-01' AND t.to_date = '9999-01-01';
SELECT DISTINCT dept_no FROM dept_emp WHERE emp_no = 10010;   -- phòng đã từng làm, tránh chọn trùng
```
2. Chạy lệnh Java. Kỳ vọng in ra:
```
Da thuyen chuyen (emp=10010, dept=d007, title=Senior Engineer): ...
```
và kết quả khớp với dòng mới mở trong DB:
```sql
SELECT de.dept_no, t.title FROM dept_emp de JOIN titles t ON de.emp_no = t.emp_no
WHERE de.emp_no = 10010 AND de.to_date = '9999-01-01' AND t.to_date = '9999-01-01';
```

## Verify nhánh lỗi
- Chạy lại **cùng** `-Dexec.args` → proc báo `Nhân viên đã làm việc tại phòng ban này`, trong DB **1 dòng mở** duy nhất (rollback hoạt động).
- `new_title = Manager` → báo lỗi không bổ nhiệm Manager.
- Java tự kiểm tra trước (PreparedStatement): emp không tồn tại → `Nhân viên ... không tồn tại hoặc không đang làm việc`.

## Biết mình đang làm gì
- Chương trình **mutate dữ liệu thật** (đóng row cũ + insert row mới ở `dept_emp`/`titles`). Suy ra lỗi `Duplicate entry ... 'dept_emp.PRIMARY'` → emp đã từng ở phòng đó rồi; reload DB pristine hoặc đổi emp/dept.
- `Thuyên chuy?n th?t b?i` / chữ hiển thị lạ chỉ là console Windows codepage khi in tiếng Việt — không phải lỗi.