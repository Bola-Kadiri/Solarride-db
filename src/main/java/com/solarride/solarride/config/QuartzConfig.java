package com.solarride.solarride.config;

import org.springframework.context.annotation.Configuration;

/**
 * Quartz is auto-configured by spring-boot-starter-quartz with JDBC job store
 * and Spring DI support. Properties are in application.yml under spring.quartz.
 */
@Configuration
public class QuartzConfig {
    // Spring Boot Quartz auto-configuration handles everything.
    // Clustering, job store type, and schema init are controlled via application.yml.
}