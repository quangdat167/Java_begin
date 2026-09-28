package vn.dangquangdat.javabegin.learning.day08;

import java.util.Map;

/** Ngay 8: SOLID qua Strategy + Factory thay vi if/else dai. */
public class DesignPatternsDemo {

    public static void main(String[] args) {
        NotificationFactory factory = new NotificationFactory(Map.of(
                Channel.EMAIL, new EmailNotification(),
                Channel.DASHBOARD, new DashboardNotification()
        ));

        factory.forChannel(Channel.EMAIL).send("dat@example.com", "Agent completed");
        factory.forChannel(Channel.DASHBOARD).send("user-01", "New datasource was shared");
    }

    interface NotificationStrategy {
        void send(String receiver, String message);
    }

    static final class EmailNotification implements NotificationStrategy {
        public void send(String receiver, String message) {
            System.out.printf("EMAIL to %s: %s%n", receiver, message);
        }
    }

    static final class DashboardNotification implements NotificationStrategy {
        public void send(String receiver, String message) {
            System.out.printf("DASHBOARD for %s: %s%n", receiver, message);
        }
    }

    static final class NotificationFactory {
        private final Map<Channel, NotificationStrategy> strategies;

        NotificationFactory(Map<Channel, NotificationStrategy> strategies) {
            this.strategies = Map.copyOf(strategies);
        }

        NotificationStrategy forChannel(Channel channel) {
            NotificationStrategy strategy = strategies.get(channel);
            if (strategy == null) {
                throw new IllegalArgumentException("Unsupported channel: " + channel);
            }
            return strategy;
        }
    }

    enum Channel { EMAIL, DASHBOARD }
}

