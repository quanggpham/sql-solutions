# TEST — Câu 1 (các query SELECT)

## Chuẩn bị
- Load CSDL `employees` (datacharmer/test_db) — bản sạch; 1.x chỉ đọc nên không sửa dữ liệu.
- Chạy từ repo root, từng file:

```
mysql -u root -p employees < 1/1.2.sql
```

- PowerShell không hỗ trợ redirection `<`: `cmd /c "mysql -u root -p employees < 1/1.2.sql"`, hoặc `Get-Content 1/1.2.sql -Raw | mysql -u root -p employees`.

## Kết quả kỳ vọng

| File | Hình dạng kết quả | Kiểm tra nhanh |
| :--- | :--- | :--- |
| 1.1 | mỗi phòng ban 1 nhân viên tốt nghiệp... tuyển từ 1995, kỳ lương 25/07/1996 | số dòng ≤ 9 (số phòng), mỗi `dept_no` 1 dòng |
| 1.2 | 1 phòng duy nhất | `avg_salary` của dòng trả về ≥ avg của mọi phòng khác (dept `Sales` có avg cao nhất trong data gốc) |
| 1.3 | đúng 1 dòng `emp_no = 10005` | `salary_total` > 0 |
| 1.4 | 1 giá trị đếm | count ≥ 1 (tổng số người làm dưới quyền Margareta Markovitch suốt thời gian quản lý) |
| 1.5 | ≤ 6 dòng (mỗi `title` 1 dòng) kỳ 25/07/1996 | mỗi title 1 `avg_salary` |
| 1.6 | 1 giá trị tổng thuế | > 0 (tính theo biểu thuế lũy tiến kỳ 25/07/1996) |
| 1.7 | ≤ 9 dòng (mỗi phòng 1) cả năm 1996 | phòng không có ai làm trong 1996 sẽ không xuất hiện |
| 1.8 | danh sách nam | mọi dòng `gender = 'M'` và chỉ xuất hiện đúng 1 trong 2 phòng `d002`/`d003` |
| 1.9 | các phòng có tổng > 300k$ kỳ 25/07/1996 | mỗi dòng có `total_salary > 300000` |
| 1.10 | các `emp_no` đang làm việc | mỗi emp đang giữ ≥ 2 chức vụ (titles) hiện tại |
| 1.11 | mỗi phòng ≤ 5 dòng | tất cả `title = 'Staff'`, mỗi phòng đúng 5 (hoặc ít hơn nếu phòng không đủ) |
| 1.12 | các emp | mỗi emp từng làm 2 phòng khác nhau, với title khác nhau tương ứng |
| 1.13 | các nhóm `dept+title+exp_year` | cột `avg_gap` = `avg_male_salary - avg_female_salary` |

## Ghi chú
- Hầu hết query phải lọc lịch sử bằng sentinel `to_date = '9999-01-01'` ở `salaries`/`titles`/`dept_emp`/`dept_manager`. Kết quả rỗng hoặc sai số dòng → kiểm tra lại điều kiện này trước.