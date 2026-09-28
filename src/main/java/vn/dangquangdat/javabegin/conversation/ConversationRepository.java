package vn.dangquangdat.javabegin.conversation;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    Page<Conversation> findByOwnerEmailAndTitleContainingIgnoreCaseOrderByUpdatedAtDesc(
            String ownerEmail,
            String keyword,
            Pageable pageable
    );

    Optional<Conversation> findByIdAndOwnerEmail(Long id, String ownerEmail);
}

