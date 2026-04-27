package com.nightout.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

/**
 * A tag/label that can be attached to a Night to describe its vibe.
 * Examples: "House Music", "Live DJ", "Ladies Night", "18+", "Dress Code"
 *
 * Tags are reused across many nights — this is why it's ManyToMany.
 * The join table "night_tags" is defined on the Night side.
 */
@Entity
@Table(name = "tags")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tag extends BaseEntity {

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    /**
     * Inverse side of the Night ↔ Tag ManyToMany.
     * mappedBy = "tags" — the 'tags' field on Night owns the join table.
     */
    @ManyToMany(mappedBy = "tags", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<Night> nights = new HashSet<>();
}