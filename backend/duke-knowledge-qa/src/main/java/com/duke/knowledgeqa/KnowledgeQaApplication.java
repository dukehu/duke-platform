package com.duke.knowledgeqa;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@MapperScan("com.duke.knowledgeqa.mapper")
@ConfigurationPropertiesScan("com.duke.knowledgeqa.config.properties")
public class KnowledgeQaApplication {

    public static void main(String[] args) {
        SpringApplication.run(KnowledgeQaApplication.class, args);
    }
}
