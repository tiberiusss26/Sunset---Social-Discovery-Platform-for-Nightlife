package com.nightout.repository;

import com.nightout.domain.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RoleRepository extends JpaRepository<Role, UUID> {

    Optional<Role> findByName(Role.RoleName name);

    boolean existsByName(Role.RoleName name);
}