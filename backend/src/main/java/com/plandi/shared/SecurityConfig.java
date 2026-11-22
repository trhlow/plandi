package com.plandi.shared;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
class SecurityConfig {

    // Task 6 thêm đăng nhập, phiên và CSRF cho /api/**. Ngoài API chỉ là file tĩnh của web/dist, ai cũng tải được.
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http.authorizeHttpRequests(auth -> auth.requestMatchers("/actuator/health")
                        .permitAll()
                        .requestMatchers("/api/**", "/actuator/**")
                        .denyAll()
                        .anyRequest()
                        .permitAll())
                .build();
    }
}
