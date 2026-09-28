package vn.dangquangdat.javabegin.learning.day09;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Ngay 9: mot service nho, pure function, rat de viet unit test. */
public class PromotionService {

    public BigDecimal applyPercent(BigDecimal originalPrice, int percentOff) {
        if (originalPrice == null || originalPrice.signum() < 0) {
            throw new IllegalArgumentException("originalPrice must be non-negative");
        }
        if (percentOff < 0 || percentOff > 100) {
            throw new IllegalArgumentException("percentOff must be between 0 and 100");
        }

        BigDecimal multiplier = BigDecimal.valueOf(100L - percentOff)
                .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        return originalPrice.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
    }
}

