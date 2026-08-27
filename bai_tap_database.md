# BÀI TẬP CƠ SỞ DỮ LIỆU & LẬP TRÌNH HỆ THỐNG

---

## CÂU 1: TRUY VẤN DỮ LIỆU (SQL QUERIES)

> **Quy ước chung về tính lương:**  
> - Ngày nhận lương cố định là **ngày 25 hàng tháng**.  
> - Bảng `salaries` lưu khoảng thời gian từ `from_date` đến `to_date` là chu kỳ áp dụng mức lương `salary`.  
> - Lương tháng của nhân viên (`emp_no`) được tính theo bản ghi có khoảng thời gian `[from_date, to_date]` chứa **ngày 25** của tháng đó.

* **1.1.** Liệt kê nhân viên bắt đầu làm việc từ năm **1995** có mức lương thấp nhất ở mỗi phòng ban (xét kỳ lương **25/07/1996**).
* **1.2.** Tìm phòng ban có **mức lương trung bình cao nhất**.
* **1.3.** Lấy thông tin của nhân viên có `emp_no = 10005` gồm: `first_name`, `last_name`, `hire_date`, `salary_total`.  
  *(Trong đó `salary_total` là tổng lương nhận được trong toàn bộ thời gian giữ chức vụ **"Staff"** theo bảng `titles`).*
* **1.4.** Đếm tổng số nhân viên mà quản lý **Margareta Markovitch** đã từng quản lý trong suốt thời gian giữ chức vụ (tính cả nhân viên làm việc dưới quyền dù chỉ 1 ngày).
* **1.5.** Tính mức **lương trung bình** phải trả cho từng nhóm chức danh (`title`) trong kỳ lương **25/07/1996**.
* **1.6.** Tính tổng **thuế thu nhập** toàn công ty phải nộp trong kỳ lương **25/07/1996** theo biểu thuế lũy tiến:

  | Mức thu nhập | Thuế suất áp dụng | Ghi chú |
  | :--- | :---: | :--- |
  | `[0 -> 40k]` | **0%** | Miễn thuế |
  | `(40k -> 60k]` | **5%** | Tính 5% cho phần vượt trên 40k |
  | `(60k -> 90k]` | **10%** | Tính 10% cho phần vượt trên 60k |
  | `> 90k` | **15%** | Tính 15% cho phần vượt trên 90k |

  *(Quy ước: `[` là lấy dấu bằng, `(` là lớn hơn).*

* **1.7.** Tính tổng lương thực tế phải chi trả cho từng phòng ban trong cả năm **1996**.
* **1.8.** Liệt kê danh sách các nhân viên nam **chỉ làm việc tại một trong hai phòng ban** `d002` hoặc `d003` *(loại trừ những nhân viên đã từng làm việc ở cả hai phòng ban này)*.
* **1.9.** Tính tổng quỹ lương của mỗi phòng ban trong kỳ lương **25/07/1996** và lọc ra các phòng ban có tổng chi trả **lớn hơn 300.000$**.
* **1.10.** Liệt kê mã nhân viên (`emp_no`) của những người đang làm việc và **đang giữ từ 2 chức vụ trở lên** (chức vụ hiện tại được định danh bởi `to_date = '9999-01-01'`).
* **1.11.** Tại mỗi phòng ban, tìm **Top 5 nhân viên** đang làm việc giữ chức danh **"Staff"** có mức lương cao nhất.
* **1.12.** Tìm các nhân viên đã từng làm việc ở **2 phòng ban khác nhau**, đồng thời đảm nhận **chức vụ khác nhau** tương ứng ở mỗi phòng.
* **1.13.** Thống kê và **so sánh mức lương trung bình giữa Nam và Nữ** theo từng nhóm: cùng cấp độ chức vụ, cùng phòng ban và cùng số năm kinh nghiệm.

---

## CÂU 2: NHÓM TRUY VẤN CẬP NHẬT DỮ LIỆU (TRANSACTIONS)

*Mỗi bài tập dưới đây được thực thi như một transaction hoàn chỉnh, đảm bảo tính toàn vẹn dữ liệu (ACID).*

* **2.1.** Thăng chức cho nhân viên có `emp_no = 10002` từ chức vụ **"Staff"** lên **"Senior Staff"**.  
  *Yêu cầu:* Đóng/dừng chức vụ hiện tại (cập nhật `to_date`) trước khi tạo bản ghi chức vụ mới.
* **2.2.** Thực hiện xóa phòng ban **"Production"** cùng toàn bộ nhân viên trực thuộc và tất cả các dữ liệu liên quan ở các bảng phụ thuộc.
* **2.3.** Thêm phòng ban mới có tên **"Bigdata & ML"**, đồng thời bổ nhiệm nhân viên có mã `emp_no = 10173` làm Trưởng phòng (Manager).

---

## CÂU 3: STORED PROCEDURE TRA CỨU NHÂN VIÊN

Viết một Stored Procedure nhận tham số đầu vào là **tên nhân viên** (`first_name` hoặc `last_name`). Trả về đồng thời **2 tập kết quả (Result Sets)** trong một lần gọi:

* **Kết quả 1 (Thông tin hồ sơ):** `emp_no`, `full_name`, `gender`, `title` (chức vụ hiện tại), `dept_name` (tên phòng ban hiện tại).
* **Kết quả 2 (Thu nhập):** Tính tổng lương tích lũy của từng nhân viên mang tên đó kể từ lúc bắt đầu làm việc đến thời điểm hiện tại.

---

## CÂU 4: STORED PROCEDURE THUYÊN CHUYỂN PHÒNG BAN

Viết Stored Procedure thực hiện điều chuyển công tác cho một nhân viên sang phòng ban mới với chức danh mới *(không áp dụng bổ nhiệm làm Quản lý/Manager)*.  
*Kết quả trả về gồm:* `emp_no`, `full_name`, `gender`, `title` (mới), `dept_name` (mới).

---

## CÂU 5: LẬP TRÌNH JAVA - GỌI STORED PROCEDURE

Viết hàm trong Java sử dụng **Connection Pool** (ví dụ HikariCP) và **CallableStatement / PreparedStatement** để thực thi Stored Procedure ở **Câu 4**.

* **Tham số đầu vào:**
  1. Mã nhân viên (`emp_no`)
  2. Mã phòng ban mới (`dept_no`)
  3. Chức danh mới (`new_title`)
* **Yêu cầu kỹ thuật:** Connection phải được trả về pool an toàn, đối tượng Statement và ResultSet phải được giải phóng/đóng trong khối `finally` (hoặc `try-with-resources`).

---

## CÂU 6: LẬP TRÌNH JAVA - TRANSACTION MANAGEMENT

Xây dựng mã nguồn Java quản lý Transaction để thực thi chuỗi lệnh của **Câu 2.1**.  
*Yêu cầu:* Tắt chế độ `auto-commit`, nếu có bất kỳ ngoại lệ (Exception) nào xảy ra thì thực hiện `rollback()` toàn bộ dữ liệu, chỉ `commit()` khi tất cả thao tác thành công.

---

## CÂU 7: XÂY DỰNG RESTFUL WEBSERVICE TRA CỨU NHÂN VIÊN

Xây dựng một Webservice trả về định dạng **JSON** danh sách nhân viên từ bảng `employees`. API hỗ trợ lọc dữ liệu linh hoạt theo các Query Parameters:

| Tham số | Ý nghĩa | Điều kiện lọc |
| :--- | :--- | :--- |
| `hire_date_from` | Ngày bắt đầu làm việc | `hire_date >= hire_date_from` |
| `salary` | Mức lương | Lương hiện tại `>= salary` |
| `dept_no` | Mã phòng ban | Đang trực thuộc phòng ban này |
| `title` | Chức danh | Chức danh hiện tại |

> *Lưu ý:* Cả 4 tham số đều là tùy chọn (optional). Chỉ áp dụng điều kiện lọc tương ứng nếu tham số được truyền vào.

---

## CÂU 8: HỆ THỐNG WEBSERVICE QUẢN LÝ NHÂN SỰ & TIỀN LƯƠNG

Thiết kế và triển khai các API và tác vụ nền cho ứng dụng Quản lý nhân viên:

### 1. Nhóm API Nghiệp Vụ Nhân Sự
* **`POST /employees`**: Tiếp nhận và khởi tạo nhân viên mới.
  * Dữ liệu nhận vào: Ngày sinh, Họ và tên (backend tự tách thành `first_name` và `last_name`), Giới tính, Ngày bắt đầu làm việc (`hire_date`), Chức danh ban đầu, Mức lương ban đầu.
* **`PUT /employees/transfer`**: Điều chuyển nhân sự sang phòng ban khác.
* **`GET /payroll/summary`**: Truy vấn bảng lương theo tháng/năm. Trả về đồng thời:
  * Chi tiết lương phải trả cho từng nhân viên.
  * Tổng quỹ lương của từng phòng ban.
  * Tổng quỹ lương chi trả trên toàn công ty.

### 2. Tác Vụ Tự Động Định Kỳ (Background Job / Thread)
* **Lịch chạy:** Tự động thực thi vào lúc **23:59 ngày 25 hàng tháng**.
* **Nghiệp vụ:** Tính tổng thuế TNCN của toàn bộ nhân viên trong kỳ lương theo công thức ở **Câu 1.6** và lưu vào bảng `income_tax`.
* **Cấu trúc bảng `income_tax`:**
  * `emp_no`: Mã nhân viên
  * `tax`: Số tiền thuế TNCN
  * `month`: Tháng kết toán
  * `year`: Năm kết toán
  *(Hệ thống cần tự động kiểm tra sự tồn tại của bảng và tự động tạo mới nếu bảng chưa tồn tại trước khi insert).*

### 3. Nhóm API Phân Tích & Đánh Giá Nâng Cao
* **`GET /analytics/unusual-promotions` (Phân tích thăng tiến bất thường):**
  * Lọc nhân viên có tối thiểu **4 lần thay đổi chức vụ**.
  * Có ít nhất 1 lần bị **"giáng chức"** (ví dụ: Senior về Staff).
  * Chức vụ hiện tại cao hơn chức vụ khởi điểm.
  * Sắp xếp theo **tốc độ phục hồi** (thời gian từ lúc bị giáng chức đến khi vượt qua chức vụ ban đầu).
* **`GET /analytics/promotion-proposals` (Đề xuất thăng chức):**  
  Tính điểm đánh giá nhân sự theo 4 tiêu chí trọng số:
  1. *Thời gian giữ chức vụ hiện tại (25%):* Đo lường kinh nghiệm và độ chín.
  2. *Mức lương so với đồng nghiệp (30%):* So sánh tương quan trong cùng phòng ban và cùng cấp bậc.
  3. *Khả năng dẫn dắt (20%):* Số lượng nhân viên mới gia nhập phòng ban sau thời điểm nhân viên này vào làm.
  4. *Tính ổn định (25%):* Tần suất chuyển phòng ban (càng ít chuyển phòng, điểm càng cao).  
  *Đầu ra:* Danh sách **Top 10 nhân viên** có tổng điểm cao nhất kèm chi tiết điểm thành phần.

### 4. Yêu Cầu Toàn Vẹn Dữ Liệu
* Tất cả thao tác ghi trên nhiều bảng phải được bọc trong Transaction.
* Trong thời gian tiến hành tất toán lương cuối tháng, áp dụng cơ chế khóa/cô lập để ngăn chặn việc thay đổi dữ liệu trên toàn bộ các bảng liên quan đến lương.

---

## CÂU 9: THIẾT KẾ CƠ SỞ DỮ LIỆU CỬA HÀNG TẠP HÓA

Thiết kế mô hình CSDL quan hệ đáp ứng đầy đủ các chức năng nghiệp vụ sau:

* **Quản lý tài khoản & Đăng nhập:** Phân quyền và xác thực nhân viên hệ thống.
* **Chấm công:** Ghi nhận chính xác ngày và mốc thời gian (check-in / check-out) làm việc của từng nhân viên.
* **Quản lý nhập - xuất kho:** Lưu vết từng mã hàng, số lượng, ngày nhập/xuất và định danh nhân viên thực hiện thao tác.
* **Bán hàng & Hóa đơn:** Tạo lập hóa đơn bán lẻ, lưu vết thông tin người lập đơn (nhân viên) và người mua (khách hàng).
* **Tích điểm thành viên:**
  * Cơ chế: **1.000 VNĐ chi tiêu = 1 điểm tích lũy**.
  * Tính tổng điểm từ ngày **01/01 hàng năm** đến thời điểm hiện tại.
  * Tự động đặt lại (reset) điểm tích lũy về `0` vào đầu năm mới.
