package vn.dangquangdat.javabegin.chat;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.dangquangdat.javabegin.conversation.Conversation;
import vn.dangquangdat.javabegin.conversation.ConversationService;

import java.time.Instant;
import java.util.List;

@Service
public class ChatService {
    private final ConversationService conversations;
    private final ChatTurnRepository turns;

    public ChatService(ConversationService conversations, ChatTurnRepository turns) {
        this.conversations = conversations;
        this.turns = turns;
    }

    @Transactional
    public ChatView send(ChatRequest request, String ownerEmail) {
        Conversation conversation = conversations.requireForNewTurn(request.conversationId(), ownerEmail);
        String model = request.modelName() == null ? "demo-java-model" : request.modelName();
        String answer = "[" + model + "] Java backend received: " + request.message();
        return ChatView.from(turns.save(
                new ChatTurn(conversation.getId(), ownerEmail, request.message().trim(), answer)
        ));
    }

    public List<ChatView> history(long conversationId, String ownerEmail) {
        conversations.findOne(conversationId, ownerEmail);
        return turns.findByConversationIdAndOwnerEmailOrderByCreatedAt(conversationId, ownerEmail)
                .stream().map(ChatView::from).toList();
    }

    public record ChatRequest(
            String message,
            long conversationId,
            String dataContext,
            List<String> url,
            String modelName
    ) {
    }

    public record ChatView(
            long id,
            String message,
            String response,
            boolean memoryEnabled,
            String memoryType,
            Instant createdAt,
            long chatConversationId,
            long turnId,
            List<Object> files
    ) {
        static ChatView from(ChatTurn turn) {
            return new ChatView(
                    turn.getId(), turn.getMessage(), turn.getResponse(), false, "NONE",
                    turn.getCreatedAt(), turn.getConversationId(), turn.getId(), List.of()
            );
        }
    }
}

