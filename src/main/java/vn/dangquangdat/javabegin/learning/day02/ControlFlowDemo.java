package vn.dangquangdat.javabegin.learning.day02;

import java.util.List;

/** Ngay 2: method, if/switch, loop va cach tach logic thanh ham nho. */
public class ControlFlowDemo {

    public static void main(String[] args) {
        List<RequestUsage> usages = List.of(
                new RequestUsage("FREE", 80),
                new RequestUsage("PRO", 1_200),
                new RequestUsage("ENTERPRISE", 10_000)
        );

        for (RequestUsage usage : usages) {
            String state = classify(usage.requestCount());
            long remaining = remainingQuota(usage.plan(), usage.requestCount());
            System.out.printf("%s -> %s, remaining=%d%n", usage.plan(), state, remaining);
        }
    }

    static String classify(long requestCount) {
        if (requestCount == 0) {
            return "INACTIVE";
        }
        if (requestCount < 1_000) {
            return "NORMAL";
        }
        return "HEAVY";
    }

    static long remainingQuota(String plan, long used) {
        long quota = switch (plan) {
            case "FREE" -> 100;
            case "PRO" -> 5_000;
            case "ENTERPRISE" -> 100_000;
            default -> throw new IllegalArgumentException("Unknown plan: " + plan);
        };
        return Math.max(0, quota - used);
    }

    record RequestUsage(String plan, long requestCount) {
    }
}

