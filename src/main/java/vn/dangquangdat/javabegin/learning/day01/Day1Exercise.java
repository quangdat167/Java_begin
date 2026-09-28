package vn.dangquangdat.javabegin.learning.day01;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Day1Exercise {

    public static void main(String[] args) {
        PlanPrice pro = new PlanPrice(
                "PRO",
                new BigDecimal("10"),
                "USD"
        );

        System.out.println(
                pro.yearlyPriceWithDiscount(new BigDecimal("105"))
        );
    }

    public record PlanPrice(
            String name,
            BigDecimal monthlyPrice,
            String currency
            ) {

        // TODO: name không được rỗng
        // TODO: monthlyPrice không được âm
        // TODO: currency không được rỗng
        public PlanPrice {
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("name is required");
            }
            if (monthlyPrice == null) {
                throw new IllegalArgumentException("monthlyPrice is required");
            }
            if (monthlyPrice.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("monthlyPrice must not be negative");
            }
            if (currency == null || currency.isBlank()) {
                throw new IllegalArgumentException("currency is required");
            }
        }

        public BigDecimal yearlyPriceWithDiscount(
                BigDecimal discountPercent
        ) {
            if (discountPercent == null) {
                throw new IllegalArgumentException(
                        "discountPercent is required"
                );
            }

            if (discountPercent.compareTo(BigDecimal.ZERO) < 0
                    || discountPercent.compareTo(BigDecimal.valueOf(100)) > 0) {
                throw new IllegalArgumentException(
                        "discountPercent must be between 0 and 100"
                );
            }
            // TODO:
            // 1. monthlyPrice * 12
            // 2. giảm 10%
            // 3. làm tròn 2 chữ số
            BigDecimal yearlyPrice = monthlyPrice.multiply(BigDecimal.valueOf(12));
            BigDecimal discount = yearlyPrice.multiply(discountPercent.divide(BigDecimal.valueOf(100)));
            return yearlyPrice.subtract(discount).setScale(2, RoundingMode.HALF_UP);
        }
    }
}
