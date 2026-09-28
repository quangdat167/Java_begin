package vn.dangquangdat.javabegin.chat;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "chat_turns")
public class ChatTurn {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long conversationId;

    @Column(nullable = false)
    private String ownerEmail;

    @Column(nullable = false, length = 4_000)
    private String message;

    @Column(nullable = false, length = 8_000)
    private String response;

    @Column(nullable = false)
    private Instant createdAt;

    protected ChatTurn() {
    }

    public ChatTurn(Long conversationId, String ownerEmail, String message, String response) {
        this.conversationId = conversationId;
        this.ownerEmail = ownerEmail;
        this.message = message;
        this.response = response;
    }

    @PrePersist
    void onCreate() { createdAt = Instant.now(); }

    public Long getId() { return id; }
    public Long getConversationId() { return conversationId; }
    public String getMessage() { return message; }
    public String getResponse() { return response; }
    public Instant getCreatedAt() { return createdAt; }
}

