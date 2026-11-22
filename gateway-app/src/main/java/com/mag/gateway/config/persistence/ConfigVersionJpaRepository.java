package com.mag.gateway.config.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ConfigVersionJpaRepository extends JpaRepository<ConfigVersionEntity, Long> { }
