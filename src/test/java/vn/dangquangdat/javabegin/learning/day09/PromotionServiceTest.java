package vn.dangquangdat.javabegin.learning.day09;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PromotionServiceTest {
    private final PromotionService service = new PromotionService();

    @Test
    void shouldApplyPromotionWithoutFloatingPointError() {
        BigDecimal result = service.applyPercent(new BigDecimal("19.99"), 25);
        assertEquals(new BigDecimal("14.99"), result);
    }

    @Test
    void shouldRejectPercentGreaterThanOneHundred() {
        assertThrows(IllegalArgumentException.class,
                () -> service.applyPercent(new BigDecimal("19.99"), 101));
    }
}
