package com.nightout.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Shared superclass for all entities.
 *
 * @MappedSuperclass — tells Hibernate: "don't create a table for this class,
 * but DO inherit its fields into every subclass table." So every entity that
 * extends BaseEntity gets id, createdAt, and updatedAt columns for free.
 *
 * @EntityListeners(AuditingEntityListener.class) — hooks into Hibernate's
 * lifecycle to auto-populate the @CreatedDate and @LastModifiedDate fields.
 * This only works because we added @EnableJpaAuditing to the main class.
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public abstract class BaseEntity {

    /**
     * @Id           — this field is the primary key
     * @GeneratedValue — Hibernate generates the value automatically
     * UUID is better than auto-increment Long for distributed systems:
     * - No collision risk across multiple services
     * - IDs don't reveal how many records you have
     * - Safe to generate client-side before inserting
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    /**
     * @CreatedDate — JPA Auditing sets this automatically on first INSERT.
     * updatable = false means Hibernate will never UPDATE this column.
     */
    @CreatedDate
    @Column(updatable = false, nullable = false)
    private LocalDateTime createdAt;

    /**
     * @LastModifiedDate — JPA Auditing updates this on every UPDATE.
     */
    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}