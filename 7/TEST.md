# TEST — Câu 7 (REST webservice tìm nhân viên)

## Chuẩn bị
- CSDL `employees` đang mở. JDK 17+ và Maven.

## Chạy service

```powershell
$env:DB_PASSWORD = "123456"
mvn -f 7/pom.xml spring-boot:run
```

- Đổi `DB_URL` / `DB_USER` / `DB_PASSWORD` nếu khác mặc định (`jdbc:mysql://localhost:3306/employees`, `root`).
- Service chạy ở `http://localhost:8080`, stop bằng `Ctrl+C`.

## Gọi API và dự kiến

Endpoint: `GET /employees` — **cả 4 query param đều tùy chọn**, áp dụng điều kiện khi được truyền:

| Param | Ý nghĩa | Ví dụ |
| :--- | :--- | :--- |
| `hire_date_from` | `hire_date >= hire_date_from` (ISO `yyyy-MM-dd`) | `?hire_date_from=1995-01-01` |
| `salary` | lương hiện tại `>= salary` | `?salary=60000` |
| `dept_no` | đang trực thuộc phòng ban | `?dept_no=d005` |
| `title` | chức danh hiện tại | `?title=Staff` |

Test bằng PowerShell:

```powershell
# Không lọc — danh sách toàn bộ nhân viên đang làm việc
Invoke-RestMethod "http://localhost:8080/employees"         | Select-Object -First 2

# Theo ngày tuyển
Invoke-RestMethod "http://localhost:8080/employees?hire_date_from=1995-01-01" | Measure-Object

# Theo lương hiện tại
Invoke-RestMethod "http://localhost:8080/employees?salary=120000"             | Select-Object -First 5

# Theo phòng + chức danh
Invoke-RestMethod "http://localhost:8080/employees?dept_no=d005&title=Staff"  | Measure-Object

# Kết hợp hết
Invoke-RestMethod "http://localhost:8080/employees?hire_date_from=1995-01-01&salary=60000&dept_no=d005&title=Staff" | Measure-Object
```

Kỳ vọng: JSON `[{"empNo":..., "firstName":..., "lastName":..., "hireDate":"yyyy-MM-dd", "gender":"M|F"}, ...]`.

## Kiểm chứng chéo bằng SQL
Số dòng API phải khớp đếm SQL với cùng điều kiện (chỉ tính nhân viên đang làm việc):

```sql
SELECT COUNT(DISTINCT e.emp_no)
FROM employees e
JOIN dept_emp de ON e.emp_no = de.emp_no AND de.to_date = '9999-01-01'
JOIN titles  t   ON e.emp_no = t.emp_no  AND t.to_date = '9999-01-01'
JOIN salaries s  ON e.emp_no = s.emp_no  AND s.to_date = '9999-01-01'
WHERE e.hire_date >= '1995-01-01'
  AND s.salary >= 60000
  AND de.dept_no = 'd005'
  AND t.title = 'Staff';
```

## Nhánh ngược dòng
- `salary` không phải số hoặc `hire_date_from` sai định dạng → HTTP `400 Bad Request`.
- Bỏ cả 4 param vẫn trả về danh sách (mọi nhân viên đang làm việc) — dataset ~300k nhân viên nên response to, dùng kèm param để dễ đọc.