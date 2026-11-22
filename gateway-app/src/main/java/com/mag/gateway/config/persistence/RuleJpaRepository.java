package com.mag.gateway.config.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RuleJpaRepository extends JpaRepository<RuleEntity, String> { }
