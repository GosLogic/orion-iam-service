package com.goslogic.orion.iam.domain.repository;

import com.goslogic.orion.iam.domain.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailAndTenantId(String email, Long tenantId);

    Optional<User> findByEmail(String email);

    Optional<User> findByExternalId(String externalId);

    List<User> findAllByTenantId(Long tenantId);

    boolean existsByEmailAndTenantId(String email, Long tenantId);
}
