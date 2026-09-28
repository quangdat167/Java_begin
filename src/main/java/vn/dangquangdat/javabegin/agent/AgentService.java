package vn.dangquangdat.javabegin.agent;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.dangquangdat.javabegin.common.NotFoundException;

import java.time.Instant;

@Service
@Transactional(readOnly = true)
public class AgentService {
    private final AgentRepository repository;

    public AgentService(AgentRepository repository) {
        this.repository = repository;
    }

    public Page<AgentView> findAll(String ownerEmail, int page, int limit) {
        return repository.findByOwnerEmailOrderByCreatedAtDesc(
                ownerEmail, PageRequest.of(Math.max(0, page - 1), Math.clamp(limit, 1, 100))
        ).map(AgentView::from);
    }

    @Transactional
    public AgentView create(CreateAgent request, String ownerEmail) {
        AgentDefinition entity = new AgentDefinition(
                request.name().trim(), request.description().trim(), request.taskPrompt().trim(),
                request.scheduleType(), ownerEmail
        );
        return AgentView.from(repository.save(entity));
    }

    public AgentDefinition requireOwned(long id, String ownerEmail) {
        return repository.findByIdAndOwnerEmail(id, ownerEmail)
                .orElseThrow(() -> new NotFoundException("Agent " + id + " was not found"));
    }

    public record CreateAgent(String name, String description, String taskPrompt, String scheduleType) {
    }

    public record AgentView(
            long id,
            String name,
            String description,
            String taskPrompt,
            String scheduleType,
            boolean isActive,
            Instant createdAt
    ) {
        static AgentView from(AgentDefinition entity) {
            return new AgentView(
                    entity.getId(), entity.getName(), entity.getDescription(), entity.getTaskPrompt(),
                    entity.getScheduleType(), entity.isActive(), entity.getCreatedAt()
            );
        }
    }
}

