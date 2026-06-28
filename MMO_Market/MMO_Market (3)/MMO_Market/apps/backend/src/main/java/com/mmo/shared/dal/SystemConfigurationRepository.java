package com.mmo.shared.dal;

import com.mmo.shared.model.SystemConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface SystemConfigurationRepository extends JpaRepository<SystemConfiguration, Integer> {
    Optional<SystemConfiguration> findByConfigKey(String configKey);
}
