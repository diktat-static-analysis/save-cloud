package com.saveourtool.cosv.backend.configs

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.jdbc.DataSourceBuilder
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.orm.jpa.JpaTransactionManager
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.EnableTransactionManagement
import javax.persistence.EntityManagerFactory
import javax.sql.DataSource

/**
 * Configuration for Save database
 */
@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
    basePackages = ["com.saveourtool.common.repository"],
    entityManagerFactoryRef = "saveEntityManagerFactory",
    transactionManagerRef = "saveTransactionManager")
class PersistenceSaveAutoConfiguration {
    /**
     * @return DataSource
     */
    @Bean(name = ["saveDataSource"])
    @ConfigurationProperties(prefix = "spring.second-datasource")
    fun saveDataSource(): DataSource? = DataSourceBuilder.create().build()

    /**
     * @param builder
     * @param saveDataSource
     * @return LocalContainerEntityManagerFactoryBean
     */
    @Bean(name = ["saveEntityManagerFactory"])
    fun saveEntityManagerFactory(
        builder: EntityManagerFactoryBuilder,
        @Qualifier("saveDataSource") saveDataSource: DataSource?
    ): LocalContainerEntityManagerFactoryBean? = builder
        .dataSource(saveDataSource)
        .packages("com.saveourtool.common.entities")
        .persistenceUnit("save")
        .build()

    /**
     * @param saveEntityManagerFactory
     * @return PlatformTransactionManager
     */
    @Bean(name = ["saveTransactionManager"])
    fun saveTransactionManager(
        @Qualifier("saveEntityManagerFactory") saveEntityManagerFactory: EntityManagerFactory
    ): PlatformTransactionManager? = JpaTransactionManager(saveEntityManagerFactory)
}
