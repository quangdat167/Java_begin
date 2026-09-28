package vn.dangquangdat.javabegin.agent;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

@Component
public class AgentTaskRunner {
    private final AgentExecutionStore store;

    public AgentTaskRunner(AgentExecutionStore store) {
        this.store = store;
    }

    /** Chay tren executor cua Spring; application.yml bat virtual thread cho I/O task. */
    @Async
    public void run(UUID executionId, String prompt) {
        store.running(executionId);
        try {
            Thread.sleep(Duration.ofMillis(300)); // gia lap goi model/HTTP
            store.completed(executionId, "Completed prompt: " + prompt);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Agent execution was interrupted", exception);
        }
    }
}

