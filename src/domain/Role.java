package com.nightout.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

/**
 * Represents a security role (USER, VENUE_OWNER, ADMIN).
 *
 * We define roles as entities (not just an enum) so they live in the database.
 * This means you can add new roles without redeploying the app.
 */
@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role extends BaseEntity {

    /**
     * @Enumerated(EnumType.STRING) — store the enum NAME ("ROLE_USER"),
     * not its ordinal (0, 1, 2). Always use STRING — if you ever reorder
     * the enum, ordinal-based storage breaks silently.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 50)
    private RoleName name;

    /**
     * The inverse side of the User ↔ Role ManyToMany relationship.
     * mappedBy = "roles" means: "the 'roles' field on User owns this
     * relationship and holds the join table definition."
     * We don't need to cascade here — roles outlive individual users.
     */
    @ManyToMany(mappedBy = "roles")
    @Builder.Default
    private Set<User> users = new HashSet<>();

    public enum RoleName {
        ROLE_USER,
        ROLE_VENUE_OWNER,
        ROLE_ADMIN
    }
}