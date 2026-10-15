package com.example.serviceApp.Configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class FilterConfiguration {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        var corsConfiguration = new org.springframework.web.cors.CorsConfiguration();

        corsConfiguration.setAllowedOrigins(List.of("http://localhost:5173", "http://localhost:5174"));
        corsConfiguration.setAllowedMethods(
                List.of("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS")
        );
        corsConfiguration.setAllowedHeaders(
                List.of("Authorization", "Content-Type", "X-Requested-With")
        );

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfiguration);

        return source;
    }
}
