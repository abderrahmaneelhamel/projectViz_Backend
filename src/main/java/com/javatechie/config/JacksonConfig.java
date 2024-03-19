package com.javatechie.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer customizeJackson() {
        return builder -> {
            ObjectMapper objectMapper = builder.build();
            objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
            builder.configure(objectMapper);
        };
    }
}
