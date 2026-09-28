package vn.dangquangdat.javabegin.security;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.dangquangdat.javabegin.common.ApiResponse;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminDemoController {

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/demo")
    ApiResponse<Map<String, String>> adminOnly() {
        return ApiResponse.success(Map.of("permission", "ADMIN_GRANTED"));
    }
}
