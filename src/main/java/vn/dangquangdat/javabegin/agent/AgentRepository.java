package vn.dangquangdat.javabegin.agent;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AgentRepository extends JpaRepository<AgentDefinition, Long> {
    Page<AgentDefinition> findByOwnerEmailOrderByCreatedAtDesc(String ownerEmail, Pageable pageable);
    Optional<AgentDefinition> findByIdAndOwnerEmail(long id, String ownerEmail);
}

