package vn.dangquangdat.javabegin.auth;

import java.time.Instant;

/** DTO duoc thiet ke de frontend SoftAIBox co the doc truc tiep. */
public record UserProfile(
        long id,
        String firstName,
        String lastName,
        Instant createdAt,
        Instant updatedAt,
        Instant deletedAt,
        String photo,
        RoleView role,
        StatusView status,
        String email
) {
    static UserProfile from(DemoUserService.UserAccount user) {
        long roleId = user.role() == Role.ADMIN ? 1L : 2L;
        return new UserProfile(
                user.id(),
                user.firstName(),
                user.lastName(),
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.now(),
                null,
                null,
                new RoleView(roleId, user.role().name()),
                new StatusView(1L, "ACTIVE"),
                user.email()
        );
    }

    public record RoleView(long id, String name) {
    }

    public record StatusView(long id, String name) {
    }
}

