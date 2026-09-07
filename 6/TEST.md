# TEST — Câu 6 (Java transaction — thăng chức Staff → Senior Staff)

## Chuẩn bị
- CSDL `employees` với một nhân viên **đang giữ chức vụ `'Staff'`**. Bản pristine thì `10002` đúng chuẩn (Câu 2.1).
- JDK 17+ và Maven.

## Chạy

```powershell
mvn -f 6/pom.xml compile exec:java "-Ddb.password=123456" "-Dexec.args=10002"
```

- `-Dexec.args` = `emp_no` (mặc định `10002`).
- Nhớ nháy kép `-D...` trên PowerShell.

## Verify happy path
1. Kiểm tra trước:
```sql
SELECT emp_no, title, from_date, to_date FROM titles WHERE emp_no = 10002 ORDER BY from_date;
-- Kỳ vọng: 10002 đang giữ 'Staff' với to_date = '9999-01-01'
```
2. Chạy lệnh Java. Kỳ vọng: in `Da thang chuc: ... Senior Staff ...` (không có ngoại lệ).
3. Kiểm tra sau:
```sql
SELECT emp_no, title, from_date, to_date FROM titles WHERE emp_no = 10002 ORDER BY from_date;
```
Kỳ vọng:
- dòng `'Staff'` cũ giờ có `to_date = CURDATE()` (đã đóng),
- có thêm dòng `'Senior Staff'` với `from_date = CURDATE()`, `to_date = '9999-01-01'`.

## Verify nhánh rollback
Chạy **lần 2** với cùng tham số → nhân viên giờ là `Senior Staff` nên không còn dòng `Staff` mở:
- Kỳ vọng lệnh in lỗi `LOI: ... không đang giữ chức vụ 'Staff' ... (da rollback ...)`.
- Kiểm tra DB: dữ liệu **không đổi** so với cuối bước happy path (không sinh dòng `Senior Staff` thứ hai) → rollback đã hoạt động.

```sql
SELECT COUNT(*) FROM titles WHERE emp_no = 10002;                                        -- số dòng giữ nguyên
SELECT COUNT(*) FROM titles WHERE emp_no = 10002 AND title = 'Senior Staff' AND to_date = '9999-01-01';   -- vẫn đúng 1
```

## Biết mình đang làm gì
- Chương trình **mutate dữ liệu thật**. Nếu DB của bạn đã qua các bài 4/5, `10002` có thể không còn là `Staff` → dùng nhân viên khác đang là `Staff`:
```sql
SELECT emp_no, title FROM titles WHERE title = 'Staff' AND to_date = '9999-01-01' LIMIT 10;
```
rồi `-Dexec.args=<emp_no đó>`.