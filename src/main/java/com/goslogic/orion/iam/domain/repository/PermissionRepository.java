package com.goslogic.orion.iam.domain.repository;

import com.goslogic.orion.iam.domain.model.AccessLevel;
import com.goslogic.orion.iam.domain.model.Permission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PermissionRepository extends JpaRepository<Permission, Long> {

    Optional<Permission> findByResourceAndAccessLevel(String resource, AccessLevel accessLevel);
}
