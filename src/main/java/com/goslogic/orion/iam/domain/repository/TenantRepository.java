package com.goslogic.orion.iam.domain.repository;

import com.goslogic.orion.iam.domain.model.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TenantRepository extends JpaRepository<Tenant, Long> {

    Optional<Tenant> findByExternalId(String externalId);

    boolean existsByExternalId(String externalId);

    boolean existsByRuc(String ruc);
}
