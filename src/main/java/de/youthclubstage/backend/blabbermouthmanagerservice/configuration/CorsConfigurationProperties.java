package de.youthclubstage.backend.blabbermouthmanagerservice.configuration;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties
@Data
@ConfigurationProperties(value = "cors")
public class CorsConfigurationProperties {
    private String allowedOrigins;
    private String[] allowedHeaders;
    private String[] exposedHeaders;
}
