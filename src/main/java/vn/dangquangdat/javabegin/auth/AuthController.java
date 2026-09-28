package vn.dangquangdat.javabegin.auth;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.dangquangdat.javabegin.common.ApiResponse;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final DemoUserService users;
    private final TokenService tokens;

    public AuthController(DemoUserService users, TokenService tokens) {
        this.users = users;
        this.tokens = tokens;
    }

    @PostMapping("/email/login")
    ApiResponse<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        DemoUserService.UserAccount user = users.authenticate(request.email(), request.password());
        return ApiResponse.success("Login successful", toResponse(tokens.issue(user), user));
    }

    @PostMapping("/refresh")
    ApiResponse<TokenResponse> refresh(@RequestHeader("Authorization") String authorization) {
        String refreshToken = removeBearerPrefix(authorization);
        TokenService.TokenPair pair = tokens.rotate(refreshToken, users);
        DemoUserService.UserAccount user = users.findByEmail(pair.subject());
        return ApiResponse.success("Token refreshed", toResponse(pair, user));
    }

    @GetMapping("/me")
    ApiResponse<UserProfile> me(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.success(UserProfile.from(users.findByEmail(jwt.getSubject())));
    }

    private TokenResponse toResponse(TokenService.TokenPair pair, DemoUserService.UserAccount user) {
        return new TokenResponse(
                pair.accessToken(),
                pair.refreshToken(),
                pair.accessTokenExpiresAt().toEpochMilli(),
                UserProfile.from(user)
        );
    }

    private String removeBearerPrefix(String value) {
        if (value == null || !value.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Authorization must use Bearer refresh-token");
        }
        return value.substring(7);
    }

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {
    }

    public record TokenResponse(String token, String refreshToken, long tokenExpires, UserProfile user) {
    }
}
