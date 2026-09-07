package lab8;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

// Ánh xạ lỗi nghiệp vụ sang HTTP status phù hợp.
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ErrorMessage> badRequest(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(new ErrorMessage(400, e.getMessage()));
    }

    // Khóa chính dept_emp (emp_no, dept_no) bị trùng -> chuyển về phòng ban cũ, hoặc
    // dept_no không tồn tại (ngoại khóa) khi tạo mới.
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ErrorMessage> conflict(DataIntegrityViolationException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorMessage(409, "Vi phạm ràng buộc dữ liệu: " + e.getMostSpecificCause().getMessage()));
    }

    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<ErrorMessage> notFound(ResponseStatusException e) {
        return ResponseEntity.status(e.getStatusCode()).body(new ErrorMessage(e.getStatusCode().value(), e.getReason()));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorMessage> error(Exception e) {
        return ResponseEntity.internalServerError().body(new ErrorMessage(500, e.getMessage()));
    }

    public record ErrorMessage(int status, String message) {
    }
}