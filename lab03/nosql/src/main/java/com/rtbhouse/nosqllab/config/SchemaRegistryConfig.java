package com.rtbhouse.nosqllab.config;

import org.springframework.cloud.schema.registry.client.config.SchemaRegistryClientProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class SchemaRegistryConfig {

    @Bean
    @Primary
    public SchemaRegistryClientProperties schemaRegistryClientConfiguration() {
        SchemaRegistryClientProperties properties = new SchemaRegistryClientProperties();
        return properties;
    }
}
