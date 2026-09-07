# TEST — Câu 3 (stored procedure `proc_1`)

## Chuẩn bị
- Cần `mysql` CLI client (script dùng `delimiter //`).
- CSDL `employees` bản sạch; lần đầu chạy sẽ tạo proc kèm **gọi thử `proc_1('Facello')`**.

## Chạy

```
cmd /c "mysql -u root -p employees < 3/3.1.sql"
```

(Giống 2/2.x: PowerShell cần `cmd /c` cho redirection `<`.)

## Kết quả kỳ vọng
Script tự chạy `call proc_1('Facello')` — trả về **2 result sets** trong 1 lần gọi:

1. **Hồ sơ:** `emp_no, full_name, gender, title, dept_name` — các nhân viên có first/last name `Facello` **đang làm việc** (title/dept hiện tại qua `to_date='9999-01-01'`).
2. **Thu nhập:** `emp_no, full_name, total_salary` — tổng lương tích lũy từ đầu đến hiện tại, tính theo chu kỳ lương (ngày 25).

## Kiểm chứng
Số dòng result set 1 phải khớp số nhân viên tên `Facello` trong DB:

```sql
SELECT COUNT(*) FROM employees WHERE first_name = 'Facello' OR last_name = 'Facello';
```

Kiểm tra chéo cộng dồn lương: với 1 emp bất kỳ trả về, tổng của `salary * số tháng` theo chu kỳ 25 trong `salaries` phải bằng `total_salary` tương ứng.

## Chạy lại với tên khác

```sql
call proc_1('Slobodan');   -- hoặc tên bất kỳ
```

Dùng tên không tồn tại → cả 2 result set đều rỗng (không lỗi).