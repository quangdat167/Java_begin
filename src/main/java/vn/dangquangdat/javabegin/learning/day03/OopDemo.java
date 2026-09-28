package vn.dangquangdat.javabegin.learning.day03;

import java.math.BigDecimal;
import java.util.List;

/** Ngay 3: encapsulation, interface, polymorphism, enum va record. */
public class OopDemo {

    public static void main(String[] args) {
        List<PaymentGateway> gateways = List.of(new StripeGateway(), new BankTransferGateway());
        Subscription subscription = new Subscription("sub-01", Plan.PRO, new BigDecimal("29.90"));

        for (PaymentGateway gateway : gateways) {
            PaymentResult result = gateway.charge(subscription);
            System.out.println(result.message());
        }
    }

    interface PaymentGateway {
        PaymentResult charge(Subscription subscription);
    }

    static final class StripeGateway implements PaymentGateway {
        @Override
        public PaymentResult charge(Subscription subscription) {
            return new PaymentResult(true, "Stripe charged " + subscription.amount());
        }
    }

    static final class BankTransferGateway implements PaymentGateway {
        @Override
        public PaymentResult charge(Subscription subscription) {
            return new PaymentResult(false, "Waiting for bank transfer of " + subscription.amount());
        }
    }

    enum Plan {
        FREE, PRO, ENTERPRISE
    }

    record Subscription(String id, Plan plan, BigDecimal amount) {
        Subscription {
            if (amount.signum() < 0) {
                throw new IllegalArgumentException("amount cannot be negative");
            }
        }
    }

    record PaymentResult(boolean successful, String message) {
    }
}

