package vn.dangquangdat.javabegin.conversation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;

@Entity
@Table(name = "chat_conversations")
public class Conversation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(nullable = false)
    private String ownerEmail;

    @Column(nullable = false)
    private int turnCount;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    protected Conversation() {
    }

    public Conversation(String title, String ownerEmail) {
        this.title = title;
        this.ownerEmail = ownerEmail;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public void rename(String newTitle) {
        this.title = newTitle;
    }

    public void increaseTurnCount() {
        turnCount++;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getOwnerEmail() { return ownerEmail; }
    public int getTurnCount() { return turnCount; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}

