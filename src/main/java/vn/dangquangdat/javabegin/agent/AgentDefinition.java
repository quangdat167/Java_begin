package vn.dangquangdat.javabegin.agent;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "agents")
public class AgentDefinition {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(nullable = false, length = 4_000)
    private String taskPrompt;

    @Column(nullable = false)
    private String scheduleType;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false)
    private String ownerEmail;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected AgentDefinition() {
    }

    public AgentDefinition(String name, String description, String taskPrompt, String scheduleType, String ownerEmail) {
        this.name = name;
        this.description = description;
        this.taskPrompt = taskPrompt;
        this.scheduleType = scheduleType;
        this.ownerEmail = ownerEmail;
        this.active = true;
    }

    @PrePersist
    void onCreate() { createdAt = Instant.now(); }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getTaskPrompt() { return taskPrompt; }
    public String getScheduleType() { return scheduleType; }
    public boolean isActive() { return active; }
    public String getOwnerEmail() { return ownerEmail; }
    public Instant getCreatedAt() { return createdAt; }
}

