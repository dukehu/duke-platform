package com.duke.demo.config;

import com.duke.demo.config.properties.QdrantProperties;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Qdrant 客户端配置
 */
@Configuration
@AllArgsConstructor
public class QdrantConfig {

    private final QdrantProperties qdrantProperties;

    @Bean
    public QdrantClient qdrantClient() {
        QdrantGrpcClient.Builder grpcClientBuilder = QdrantGrpcClient.newBuilder(qdrantProperties.getHost(), qdrantProperties.getPort(), false);
        if (!qdrantProperties.getApiKey().isEmpty()) {
            grpcClientBuilder.withApiKey(qdrantProperties.getApiKey());
        }
        return new QdrantClient(grpcClientBuilder.build());
    }
}