package vn.dangquangdat.javabegin.conversation;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.dangquangdat.javabegin.common.NotFoundException;

import java.time.Instant;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ConversationService {
    private final ConversationRepository repository;

    public ConversationService(ConversationRepository repository) {
        this.repository = repository;
    }

    public Page<ConversationView> findAll(String ownerEmail, String keyword, int page, int limit) {
        int safePage = Math.max(1, page) - 1;
        int safeLimit = Math.clamp(limit, 1, 100);
        return repository
                .findByOwnerEmailAndTitleContainingIgnoreCaseOrderByUpdatedAtDesc(
                        ownerEmail, keyword == null ? "" : keyword.trim(), PageRequest.of(safePage, safeLimit)
                )
                .map(ConversationView::from);
    }

    public ConversationView findOne(long id, String ownerEmail) {
        return ConversationView.from(requireOwned(id, ownerEmail));
    }

    @Transactional
    public ConversationView create(String title, String ownerEmail) {
        return ConversationView.from(repository.save(new Conversation(title.trim(), ownerEmail)));
    }

    @Transactional
    public ConversationView rename(long id, String title, String ownerEmail) {
        Conversation conversation = requireOwned(id, ownerEmail);
        conversation.rename(title.trim());
        return ConversationView.from(conversation);
    }

    @Transactional
    public void delete(long id, String ownerEmail) {
        repository.delete(requireOwned(id, ownerEmail));
    }

    @Transactional
    public Conversation requireForNewTurn(long id, String ownerEmail) {
        Conversation conversation = requireOwned(id, ownerEmail);
        conversation.increaseTurnCount();
        return conversation;
    }

    private Conversation requireOwned(long id, String ownerEmail) {
        return repository.findByIdAndOwnerEmail(id, ownerEmail)
                .orElseThrow(() -> new NotFoundException("Conversation " + id + " was not found"));
    }

    public record ConversationView(
            long id,
            String title,
            int turnCount,
            Instant createdAt,
            Instant updatedAt,
            Instant deletedAt,
            List<Object> turns,
            LastTurn lastTurn,
            long chatConversationId
    ) {
        static ConversationView from(Conversation entity) {
            return new ConversationView(
                    entity.getId(), entity.getTitle(), entity.getTurnCount(), entity.getCreatedAt(),
                    entity.getUpdatedAt(), null, List.of(), null, entity.getId()
            );
        }
    }

    public record LastTurn(String prompt, Instant time) {
    }
}

