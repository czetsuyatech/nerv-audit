package com.czetsuyatech.audit.config;

import com.czetsuyatech.audit.infrastructure.envers.entity.NervEntityConfig;
import com.czetsuyatech.audit.infrastructure.envers.repository.NervRepositoryConfig;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Test persistence configuration enabling entity scanning and custom repository base support.
 */
@EnableJpaRepositories(basePackageClasses = {NervRepositoryConfig.class})
@EntityScan(basePackageClasses = NervEntityConfig.class)
@EnableTransactionManagement
public class TestPersistenceConfig {

}
