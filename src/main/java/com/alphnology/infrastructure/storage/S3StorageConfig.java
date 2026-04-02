package com.alphnology.infrastructure.storage;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.util.StringUtils;

@Configuration
@EnableConfigurationProperties(StorageProperties.class)
public class S3StorageConfig {

    @Bean
    @Primary
    public MinioClient s3Client(StorageProperties props) {
        return createClient(props.getEndpoint(), props);
    }

    @Bean
    @Qualifier("publicS3Client")
    public MinioClient publicS3Client(StorageProperties props) {
        String endpoint = props.getPublicEndpoint() != null && !props.getPublicEndpoint().isBlank()
                ? props.getPublicEndpoint()
                : props.getEndpoint();
        return createClient(endpoint, props);
    }

    private MinioClient createClient(String endpoint, StorageProperties props) {
        MinioClient.Builder builder = MinioClient.builder()
                .endpoint(endpoint);

        if (StringUtils.hasText(props.getAccessKey()) || StringUtils.hasText(props.getSecretKey())) {
            builder.credentials(props.getAccessKey(), props.getSecretKey());
        }

        return builder.build();
    }
}
