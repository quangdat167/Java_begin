package vn.dangquangdat.javabegin.learning.day01;

import java.math.BigDecimal;

/**
 * Ngay 1: kieu du lieu, bien, String, record va entry point main.
 */
public class BasicsDemo {

    public static void main(String[] args) {
        int requestCount = 1_250;                  // primitive: luu truc tiep gia tri
        long userId = 9_000_000_001L;
        boolean active = true;
        double successRate = 98.5;                // phu hop thong ke gan dung
        BigDecimal price = new BigDecimal("19.99"); // tien te can chinh xac

        String modelName = "gpt-model";           // reference type, String la immutable
        String displayName = modelName.toUpperCase();
        UserSummary user = new UserSummary(userId, "dat@example.com", active);

        System.out.printf(
                "User=%s, model=%s, requests=%d, rate=%.1f%%, price=%s%n",
                user.email(), displayName, requestCount, successRate, price
        );

    }

    /**
     * record la data carrier bat bien, gan voi type/interface DTO ben
     * TypeScript.
     */
    public record UserSummary(long id, String email, boolean active) {

        public UserSummary {
            if (id <= 0) {
                throw new IllegalArgumentException("id must be positive");
            }
            if (email == null || email.isBlank()) {
                throw new IllegalArgumentException("email is required");
            }
        }
    }
}
