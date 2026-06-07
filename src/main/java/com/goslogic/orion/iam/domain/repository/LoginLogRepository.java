package com.goslogic.orion.iam.domain.repository;

import com.goslogic.orion.iam.domain.model.LoginLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LoginLogRepository extends JpaRepository<LoginLog, Long> {

    List<LoginLog> findAllByUserIdOrderByLoginAtDesc(Long userId);
}
