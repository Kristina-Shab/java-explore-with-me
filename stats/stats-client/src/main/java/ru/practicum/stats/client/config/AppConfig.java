package ru.practicum.stats.client.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AppConfig {
    @Bean
    public RestTemplate statsRestTemplate(@Value("${stats-server.url}") String serverUrl) {
        return new RestTemplateBuilder().rootUri(serverUrl).build();
    }
}
