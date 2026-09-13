package org.example.bookstore.configurations;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Supplies shared application collaborators, including an injectable UTC clock.
 */
@Configuration(proxyBeanMethods = false)
public class StoreConfiguration {
    /**
     * Provides a UTC clock that can be replaced by a fixed clock in isolated checkout tests.
     *
     * @return the system UTC clock
     */
    @Bean Clock clock() { return Clock.systemUTC(); }
}
