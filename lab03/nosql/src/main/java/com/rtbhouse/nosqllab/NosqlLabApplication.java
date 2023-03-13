package com.rtbhouse.nosqllab;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.schema.registry.client.EnableSchemaRegistryClient;

@SpringBootApplication
@EnableSchemaRegistryClient
public class NosqlLabApplication {

    public static void main(String[] args) {
        SpringApplication.run(NosqlLabApplication.class, args);
    }

}
