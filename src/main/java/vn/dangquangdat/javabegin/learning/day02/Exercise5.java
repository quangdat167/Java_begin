package vn.dangquangdat.javabegin.learning.day02;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Exercise5 {

    public static void main(String[] arg) {
        String rawPlan = "  pro  ";
        String rawUsage = "34003";

        String plan = parsePlan(rawPlan);
        long usage = parseUsage(rawUsage);

        validate(plan, usage);
        System.out.println("Plan: " + plan);
        System.out.println("Usage: " + usage);

        System.out.println("Total quota: " + quotaOf(plan));
        System.out.println("Remaining quota: " + remainingQuota(plan, usage));
        System.out.println("Over quota: " + overQuota(plan, usage));
        System.out.println("Money over quota: " + moneyOverquota(plan, overQuota(plan, usage)));
        System.out.println();
    }

    public record Plan(String name, long requestCount) {

    }

    static String parsePlan(String rawPlan) {
        if (rawPlan == null || rawPlan.isBlank()) {
            throw new IllegalArgumentException("Plan must not null");
        }
        return rawPlan.trim().toUpperCase();
    }

    static long parseUsage(String rawUsage) {
        if (rawUsage == null || rawUsage.isBlank()) {
            throw new IllegalArgumentException("Usage must not null");
        }
        try {
            long usage = Long.parseLong(rawUsage.trim());
            return usage;
        } catch (Exception e) {
            throw new IllegalArgumentException("Usage must be a number");
        }
    }

    static void validate(String plan, long usage) {
        if (plan == null || plan.isBlank()) {
            throw new IllegalArgumentException("Plan is required!");
        }
        if (usage < 0) {
            throw new IllegalArgumentException("Usage must not be negative");
        }

        if (!plan.equals("FREE") && !plan.equals("PRO") && !plan.equals("ENTERPRISE")) {
            throw new IllegalArgumentException("plan is FREE, PRO or ENTERPRISE.");
        }
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
                new BigDecimal("0.02");
            case "ENTERPRISE" ->
                new BigDecimal("0.01");
            default ->
                throw new IllegalArgumentException("Plan not found");
        };
        return moneyOverquota
                .multiply(BigDecimal.valueOf(overQuota)).setScale(2, RoundingMode.HALF_UP);
    }

}
