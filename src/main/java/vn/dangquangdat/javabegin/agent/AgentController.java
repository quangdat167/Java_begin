package vn.dangquangdat.javabegin.agent;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import vn.dangquangdat.javabegin.common.ApiResponse;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/agent")
public class AgentController {
    private final AgentService service;
    private final AgentExecutionStore executionStore;
    private final AgentTaskRunner taskRunner;

    public AgentController(AgentService service, AgentExecutionStore executionStore, AgentTaskRunner taskRunner) {
        this.service = service;
        this.executionStore = executionStore;
        this.taskRunner = taskRunner;
    }

    @GetMapping
    ApiResponse<Map<String, List<AgentService.AgentView>>> findAll(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            Authentication authentication
    ) {
        Page<AgentService.AgentView> result = service.findAll(authentication.getName(), page, limit);
        return ApiResponse.success(Map.of("agents", result.getContent()));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ApiResponse<AgentService.AgentView> create(
            @Valid @RequestBody CreateAgentRequest request,
            Authentication authentication
    ) {
        AgentService.CreateAgent command = new AgentService.CreateAgent(
                request.name(), request.description(), request.taskPrompt(), request.scheduleType()
        );
        return ApiResponse.success("Agent created", service.create(command, authentication.getName()));
    }

    @PostMapping("/{id}/executions")
    @ResponseStatus(HttpStatus.ACCEPTED)
    ApiResponse<AgentExecutionStore.ExecutionView> execute(
            @PathVariable long id,
            Authentication authentication
    ) {
        AgentDefinition agent = service.requireOwned(id, authentication.getName());
        UUID executionId = executionStore.create(agent.getId());
        taskRunner.run(executionId, agent.getTaskPrompt());
        return ApiResponse.success("Agent execution accepted", executionStore.get(executionId));
    }

    @GetMapping("/{agentId}/executions/{executionId}")
    ApiResponse<AgentExecutionStore.ExecutionView> executionStatus(
            @PathVariable long agentId,
            @PathVariable UUID executionId,
            Authentication authentication
    ) {
        service.requireOwned(agentId, authentication.getName());
        AgentExecutionStore.ExecutionView execution = executionStore.get(executionId);
        if (execution.agentId() != agentId) {
            throw new IllegalArgumentException("Execution does not belong to this agent");
        }
        return ApiResponse.success(execution);
    }

    public record CreateAgentRequest(
            @NotBlank @Size(max = 100) String name,
            @NotBlank @Size(max = 500) String description,
            @NotBlank @Size(max = 4_000) @JsonProperty("task_prompt") String taskPrompt,
            @NotBlank @JsonProperty("schedule_type") String scheduleType
    ) {
    }
}

