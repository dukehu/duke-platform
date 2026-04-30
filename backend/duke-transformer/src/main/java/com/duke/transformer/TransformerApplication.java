package com.duke.transformer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication
public class TransformerApplication {

    public static void main(String[] args) {
        SpringApplication.run(TransformerApplication.class, args);
    }

}
