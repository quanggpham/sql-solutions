# TEST — Câu 8 (HR & payroll webservice)

## Chuẩn bị
- CSDL `employees` sạch đang mở, JDK 17+, Maven. Với DB đã bị test ở Câu 2/4/6, nạp lại dữ liệu gốc (test_db) hoặc kiểm tra trên nhân viên chưa bị đụng.

## Trước khi chạy
- `mvn -f 8/pom.xml test` — 12 unit test cho logic thuế (Câu 1.6), phân tích thăng chức và tách tên (không cần DB).
- Khởi động: `mvn -f 8/pom.xml spring-boot:run`, đặt `$env:DB_PASSWORD` (và `DB_URL`/`DB_USER` nếu khác mặc định). Lúc bean khởi tạo, app **tự tạo bảng `income_tax`** nếu chưa có.

## Gọi API và dự kiến

### 1) POST /employees — tạo nhân viên mới
```powershell
$body = @{
  birthDate = "1998-05-10"; name = "Nguyễn Văn An"; gender = "M"
  hireDate  = "2024-03-01"; title = "Staff"; salary = 45000
} | ConvertTo-Json
Invoke-RestMethod -Method Post http://localhost:8080/employees -ContentType "application/json" -Body $body
```
Kỳ vọng: HTTP `201`, trả `{empNo, firstName: "Nguyễn", lastName: "Văn An", ...}`. Với DB đã có dữ liệu, `empNo = MAX(emp_no)+1`. Kiểm chứng: `SELECT * FROM employees WHERE emp_no = <empNo>;` kèm 1 dòng `titles`, `salaries`, `dept_emp` cùng `to_date='9999-01-01'` (phòng mặc định `d005` nếu không truyền `deptNo`).

### 2) PUT /employees/transfer — điều chuyển nhân sự
```powershell
$body = @{ empNo = <empNo>; deptNo = "d004"; title = "Engineer" } | ConvertTo-Json
Invoke-RestMethod -Method Put http://localhost:8080/employees/transfer -ContentType "application/json" -Body $body
```
Kỳ vọng: HTTP `200`, trả `{empNo, fullName, gender, deptNo, deptName, title}`. Bỏ `title` trong body → chỉ chuyển phòng, giữ nguyên chức danh.
- Cùng ràng buộc như `proc_2` (Câu 4): `title = "Manager"` → `400`, chuyển về phòng ban hiện tại → `400`, `empNo`/`deptNo` không tồn tại → `404`, quay lại phòng ban đã từng làm (PK `(emp_no, dept_no)` trùng) → `409`. Tài khoản nên tạo exception qua **transaction** — thử chuyển về phòng cũ để thấy các câu lệnh ghi trước đó bị rollback (kiểm tra `dept_emp` hiện tại không bị sửa).

### 3) GET /payroll/summary — bảng lương theo kỳ
```powershell
Invoke-RestMethod "http://localhost:8080/payroll/summary?year=1996&month=7"
```
Kỳ vọng: `employees` (chi tiết từng nhân viên có kỳ lương chứa ngày 25/07/1996: `salary`, `tax`), `departments` (tổng quỹ lương + thuế từng phòng), `salaryTotal`/`taxTotal` toàn công ty. Trước khi chạy tác vụ thuế, `tax` = `null`.
- Kiểm chứng chéo (tổng quỹ lương):
```sql
SELECT SUM(s.salary) FROM salaries s
WHERE s.from_date <= '1996-07-25' AND s.to_date >= '1996-07-25';
```
- `month`/`year` sai (ngoài 1–12 / 1900–2100) → `400`.

### 4) POST /payroll/tax/run — tính thuế (mô phỏng tác vụ nền)
```powershell
Invoke-RestMethod -Method Post "http://localhost:8080/payroll/tax/run?year=1996&month=7"
```
Kỳ vọng: `{year: 1996, month: 7, employees: <số nhân viên có kỳ lương>, totalTax: <tổng thuế>}`. Bảng `income_tax` được ghi/ghi đè `(emp_no, tax, month, year)`; chạy lại lần 2 cùng tháng → cập nhật (upsert), chạy lại `GET /payroll/summary?year=1996&month=7` thấy cột `tax` đầy đủ.
- Tác vụ tự động chạy 23:59 ngày **25 hàng tháng** (`@Scheduled(cron = "0 59 23 25 * ?")`); chờ đến giờ đó hoặc gọi endpoint ở trên để kiểm tra. Khi tất toán, `SELECT ... FOR UPDATE` khóa các dòng `salaries` của kỳ (tác vụ được bọc transaction) ngăn sửa đổi song song.

### 5) GET /analytics/unusual-promotions — thăng tiến bất thường
```powershell
Invoke-RestMethod "http://localhost:8080/analytics/unusual-promotions"
```
Kỳ vọng: danh sách (xếp theo thời gian phục hồi tăng dần — nhanh nhất trước) gồm nhân viên: ≥ 4 lần thay chức vụ, ≥ 1 lần giáng chức (thứ bậc: Staff < Senior Staff < Assistant Engineer < Engineer < Senior Engineer < Technique Leader < Manager), chức vụ hiện tại > chức vụ khởi điểm. `recoveryDays` = số ngày từ lần giáng chức đầu tiên tới lần đầu đạt chức vụ cao hơn chức vụ khởi điểm.
- Kiểm chứng mẫu bằng SQL: nhân viên 10005 (đổi 3 lần) không đạt; muốn có ca giáng chức thật, tạo tạm nhân viên A→Senior Staff→Staff→... rồi gọi lại.

### 6) GET /analytics/promotion-proposals — đề xuất thăng chức
```powershell
Invoke-RestMethod "http://localhost:8080/analytics/promotion-proposals"
```
Kỳ vọng: Top 10 theo điểm giảm dần, kèm chi tiết 4 tiêu chí (đã chuẩn hóa về [0,1]):
1. `tenureMonths` — thời gian giữ chức danh hiện tại **25%**,
2. `peerRatio` — lương / trung bình lương cùng phòng ban và cùng chức danh **30%**,
3. `leadership` — số nhân viên gia nhập cùng phòng sau ngày vào làm của họ **20%**,
4. `stability` — `1/(1+số lần chuyển phòng)` **25%**.

Ghi chú: tiêu chí "dẫn dắt" ước lượng bằng số người cùng phòng hiện tại có `hire_date` lớn hơn nhân viên này (dữ liệu mẫu không có quan hệ quản lý nên không đếm cấp dưới thực sự).

## Lưu ý khi demo điểm
- Các bước 1–2 **ghi dữ liệu**: chạy trên CSDL sạch hoặc tạo nhân viên tạm riêng (bước 1) để không phá trả lời các câu 1.x.
- Nếu app không khởi động được: dựng đúng `DB_URL` hoặc lỗi "Unknown database" — nhớ DB mẫu tên `employees`.