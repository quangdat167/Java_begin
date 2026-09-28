package vn.dangquangdat.javabegin.auth;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * User store in-memory de tap trung hoc Security/JWT.
 * Production nen dung JPA, unique index cho email va migration tool.
 */
@Service
public class DemoUserService {
    private final PasswordEncoder passwordEncoder;
    private final Map<String, UserAccount> users;

    public DemoUserService(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
        this.users = Map.of(
                "dat@softaibox.local", new UserAccount(
                        1L, "dat@softaibox.local", passwordEncoder.encode("java123"),
                        "Quang", "Dat", Role.USER
                ),
                "admin@softaibox.local", new UserAccount(
                        2L, "admin@softaibox.local", passwordEncoder.encode("admin123"),
                        "SoftAIBox", "Admin", Role.ADMIN
                )
        );
    }

    public UserAccount authenticate(String email, String rawPassword) {
        UserAccount user = findByEmail(email);
        if (!passwordEncoder.matches(rawPassword, user.passwordHash())) {
            throw new IllegalArgumentException("Email or password is incorrect");
        }
        return user;
    }

    public UserAccount findByEmail(String email) {
        UserAccount user = users.get(email.toLowerCase());
        if (user == null) {
            throw new IllegalArgumentException("Email or password is incorrect");
        }
        return user;
    }

    public record UserAccount(
            long id,
            String email,
            String passwordHash,
            String firstName,
            String lastName,
            Role role
    ) {
    }
}

