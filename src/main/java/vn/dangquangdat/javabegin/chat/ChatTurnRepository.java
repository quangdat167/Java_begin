package vn.dangquangdat.javabegin.chat;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatTurnRepository extends JpaRepository<ChatTurn, Long> {
    List<ChatTurn> findByConversationIdAndOwnerEmailOrderByCreatedAt(long conversationId, String ownerEmail);
}

