package com.nightout;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Application entry point.
 *
 * @SpringBootApplication is a shortcut for three annotations:
 *
 *   @Configuration       — this class can define @Bean methods
 *   @EnableAutoConfiguration — Spring Boot inspects your classpath and
 *                              automatically configures beans (e.g. sees
 *                              PostgreSQL driver → sets up DataSource)
 *   @ComponentScan       — scans this package and all sub-packages for
 *                          @Component, @Service, @Repository, @Controller etc.
 *
 * @EnableCaching  — activates Spring's caching proxy. Any method annotated
 *                   with @Cacheable will have its return value stored in Redis
 *                   and returned from cache on subsequent calls.
 *
 * @EnableJpaAuditing — allows entities to automatically populate @CreatedDate
 *                      and @LastModifiedDate fields without manual code.
 *
 * @EnableAsync — allows methods annotated with @Async to run in a separate
 *                thread pool. We use this for sending notifications without
 *                blocking the HTTP response.
 */
@SpringBootApplication
@EnableCaching
@EnableJpaAuditing
@EnableAsync
public class NightOutApplication {

    public static void main(String[] args) {
        // SpringApplication.run() bootstraps the entire Spring context:
        // 1. Reads configuration files
        // 2. Creates all beans (services, repositories, controllers...)
        // 3. Starts the embedded Tomcat server on port 8080
        SpringApplication.run(NightOutApplication.class, args);
    }
}