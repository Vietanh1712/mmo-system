package com.mmo.shared.config;
import com.mmo.shared.model.Transaction;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableJpaRepositories(basePackages = {"com.mmo.shared.dal"})
@EntityScan(basePackages = {"com.mmo.shared.model"})
@EnableTransactionManagement
public class JpaConfig {
}

