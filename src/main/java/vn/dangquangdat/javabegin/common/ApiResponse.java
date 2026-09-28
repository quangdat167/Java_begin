package vn.dangquangdat.javabegin.common;

import org.springframework.data.domain.Page;

import java.util.List;

public record ApiResponse<T>(String code, String message, T data, ApiMeta meta) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>("SUCCESS", "Request completed", data, ApiMeta.empty());
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>("SUCCESS", message, data, ApiMeta.empty());
    }

    public static <T> ApiResponse<List<T>> page(Page<T> page) {
        return new ApiResponse<>("SUCCESS", "Request completed", page.getContent(), ApiMeta.from(page));
    }

    public static ApiResponse<Void> error(String code, String message) {
        return new ApiResponse<>(code, message, null, ApiMeta.empty());
    }
}

