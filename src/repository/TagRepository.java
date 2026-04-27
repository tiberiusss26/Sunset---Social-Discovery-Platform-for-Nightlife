package com.nightout.repository;

import com.nightout.domain.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TagRepository extends JpaRepository<Tag, UUID> {

    Optional<Tag> findByNameIgnoreCase(String name);

    List<Tag> findByNameInIgnoreCase(List<String> names);

    boolean existsByNameIgnoreCase(String name);
}