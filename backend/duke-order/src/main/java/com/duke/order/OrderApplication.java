package com.duke.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.mybatis.spring.annotation.MapperScan;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@MapperScan("com.duke.order.mapper")
@ConfigurationPropertiesScan("com.duke.order.config.properties")
public class OrderApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderApplication.class, args);
    }
}



