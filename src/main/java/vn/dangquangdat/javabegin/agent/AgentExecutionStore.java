package vn.dangquangdat.javabegin.agent;

import org.springframework.stereotype.Component;
import vn.dangquangdat.javabegin.common.NotFoundException;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AgentExecutionStore {
    private final Map<UUID, ExecutionView> executions = new ConcurrentHashMap<>();

    public UUID create(long agentId) {
        UUID id = UUID.randomUUID();
        executions.put(id, new ExecutionView(id, agentId, Status.PENDING, null, Instant.now()));
        return id;
    }

    public void running(UUID id) {
        ExecutionView old = get(id);
        executions.put(id, new ExecutionView(id, old.agentId(), Status.RUNNING, null, old.createdAt()));
    }

    public void completed(UUID id, String output) {
        ExecutionView old = get(id);
        executions.put(id, new ExecutionView(id, old.agentId(), Status.COMPLETED, output, old.createdAt()));
    }

    public ExecutionView get(UUID id) {
        ExecutionView result = executions.get(id);
        if (result == null) {
            throw new NotFoundException("Execution " + id + " was not found");
        }
        return result;
    }

    public enum Status { PENDING, RUNNING, COMPLETED, FAILED }

    public record ExecutionView(UUID id, long agentId, Status status, String output, Instant createdAt) {
    }
}

