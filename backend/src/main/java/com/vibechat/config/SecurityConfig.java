package com.vibechat.config;

import com.vibechat.service.CustomOAuth2UserService;
import com.vibechat.domain.CustomOAuth2User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import com.vibechat.filter.CorrelationIdFilter;
import org.springframework.http.HttpStatus;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    private final CustomOAuth2UserService customOAuth2UserService;
    private final CorrelationIdFilter correlationIdFilter;

    public SecurityConfig(CustomOAuth2UserService customOAuth2UserService, CorrelationIdFilter correlationIdFilter, @Value("${app.cors.allowed-origins}") String allowedOrigins) {
        this.customOAuth2UserService = customOAuth2UserService;
        this.correlationIdFilter = correlationIdFilter;
        this.allowedOrigins = allowedOrigins;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .addFilterBefore(correlationIdFilter, UsernamePasswordAuthenticationFilter.class)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf
                .csrfTokenRepository(customCsrfTokenRepository())
                .ignoringRequestMatchers("/ws/**", "/api/users/guest", "/api/auth/google" , "/api/auth/me")
            )
            // API 요청은 인증 필요 시 401을 반환(리다이렉트 방지)
            .exceptionHandling(ex -> ex
                .defaultAuthenticationEntryPointFor(
                    new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                    new AntPathRequestMatcher("/api/**")
                )
            )
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/api/users/guest", "/api/auth/google", "/api/auth/me", "/api/auth/logout", "/actuator/health", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .requestMatchers("/ws/**").permitAll() // Allow WebSocket connections
                .requestMatchers("/uploads/**").permitAll() // Allow static file access
                .anyRequest().authenticated()
            );
            // Spring Security OAuth2 비활성화 - Supabase OAuth 사용
            // .oauth2Login() 제거
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // 패턴 기반 허용(동적 IP 대응). 예: http://172.30.*.*:5173
        configuration.setAllowedOriginPatterns(Arrays.asList(allowedOrigins.split(",")));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setExposedHeaders(Arrays.asList("X-CSRF-TOKEN"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public CookieCsrfTokenRepository customCsrfTokenRepository() {
        CookieCsrfTokenRepository repo = CookieCsrfTokenRepository.withHttpOnlyFalse();
        // 프론트 Axios 인터셉터가 보내는 헤더명과 일치시키기 위해 X-CSRF-TOKEN 사용
        repo.setHeaderName("X-CSRF-TOKEN");
        return repo;
    }
}

