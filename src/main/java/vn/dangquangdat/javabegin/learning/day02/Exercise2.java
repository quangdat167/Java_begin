package vn.dangquangdat.javabegin.learning.day02;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class Exercise2 {

    public static void main(String[] arg) {
        List<Plan> listPlans = List.of(
                new Plan("FREE", 541),
                new Plan("PRO", 100_000),
                new Plan("ENTERPRISE", 25_000)
        );
        List<Plan> listPlans2 = List.of(
                new Plan("FREE", -500),
                new Plan("PRO", 100000),
                new Plan("ENTERPRISE", 25_000));
        for (Plan plan : listPlans) {
            System.out.println(plan.name());
            System.out.println("Total quota: " + quotaOf(plan.name()));
            System.out.println("Remaining quota: " + remainingQuota(plan.name(), plan.requestCount()));
            System.out.println("Over quota: " + overQuota(plan.name(), plan.requestCount()));
            System.out.println("Money over quota: " + moneyOverquota(plan.name(), overQuota(plan.name(), plan.requestCount())));
            System.out.println();
        }

        long[] a = {200L, 44L, 123L};
        System.out.println("SUM: " + sum(a));

    }

    public record Plan(String name, long requestCount) {

    }

    static long quotaOf(String plan) {
        if (plan == null || plan.isBlank()) {
            throw new IllegalArgumentException("Plan is required!");
        }
        long quota = switch (plan) {
            case "FREE" ->
                100;
            case "PRO" ->
                5000;
            case "ENTERPRISE" ->
                100_000;
            default ->
                throw new IllegalArgumentException("Plan not found");
        };
        return quota;
    }

    static long remainingQuota(String plan, long used) {
        if (used < 0) {
            throw new IllegalArgumentException("Used must not be negative");
        }
        long totalQuota = quotaOf(plan);
        long remain = Math.max(totalQuota - used, 0);
        return remain;
    }

    static long overQuota(String plan, long used) {
        if (used < 0) {
            throw new IllegalArgumentException("Used must not be negative");
        }
        long totalQuota = quotaOf(plan);
        return Math.max(0, used - totalQuota);
    }

    static long sum(long... values) {
        long sum = 0;
        for (long value : values) {
            sum += value;
        }
        return sum;
    }

    static BigDecimal moneyOverquota(String plan, long overQuota) {
        if (plan == null || plan.isBlank()) {
            throw new IllegalArgumentException("plan is required");
        }
        if (overQuota < 0) {
            throw new IllegalArgumentException("Over quota must not be negative");
        }
        BigDecimal moneyOverquota = switch (plan) {
            case "FREE" ->
                new BigDecimal("0.05");
            case "PRO" ->
                new BigDecimal("0.05");
            case "ENTERPRISE" ->
                new BigDecimal("0.01");
            default ->
                throw new IllegalArgumentException("Plan not found");
        };
        return moneyOverquota
                .multiply(BigDecimal.valueOf(overQuota)).setScale(2, RoundingMode.HALF_UP);
    }

}
