package com.saveourtool.cosv.backend

import com.zaxxer.hikari.HikariDataSource
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.orm.jpa.JpaTransactionManager
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import org.springframework.transaction.PlatformTransactionManager
import javax.persistence.EntityManagerFactory
import javax.sql.DataSource

/**
 * Configuration for Vulnerability
 */
@Configuration
@ComponentScan
@EnableJpaRepositories(basePackages = ["com.saveourtool.cosv.backend.repository"])
class CosvConfiguration {
    /**
     * @param properties
     * @return HikariDataSource
     */
    @Primary
    @Bean
    @ConfigurationProperties("spring.datasource")
    fun dataSource(properties: DataSourceProperties): HikariDataSource? = properties.initializeDataSourceBuilder().type(HikariDataSource::class.java)
        .build()

    /**
     * @param builder
     * @param dataSource
     * @return LocalContainerEntityManagerFactoryBean
     */
    @Primary
    @Bean(name = ["entityManagerFactory"])
    fun entityManagerFactory(
        builder: EntityManagerFactoryBuilder,
        @Qualifier("dataSource") dataSource: DataSource?
    ): LocalContainerEntityManagerFactoryBean? = builder
        .dataSource(dataSource)
        .packages("com.saveourtool.common.entitiescosv")
        .persistenceUnit("cosv")
        .build()

    /**
     * @param entityManagerFactory
     * @return PlatformTransactionManager
     */
    @Primary
    @Bean(name = ["transactionManager"])
    fun transactionManager(
        @Qualifier("entityManagerFactory") entityManagerFactory: EntityManagerFactory
    ): PlatformTransactionManager? = JpaTransactionManager(entityManagerFactory)
}
