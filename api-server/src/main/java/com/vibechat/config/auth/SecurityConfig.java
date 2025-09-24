package com.vibechat.config.auth;

import com.vibechat.filter.CorrelationIdFilter;
import com.vibechat.service.CustomOAuth2UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig implements WebMvcConfigurer {

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    private final CustomOAuth2UserService customOAuth2UserService;
    private final CorrelationIdFilter correlationIdFilter;
    private final AuthUserArgumentResolver authUserArgumentResolver; // 의존성 주입

    public SecurityConfig(CustomOAuth2UserService customOAuth2UserService, CorrelationIdFilter correlationIdFilter, AuthUserArgumentResolver authUserArgumentResolver, @Value("${app.cors.allowed-origins}") String allowedOrigins) {
        this.customOAuth2UserService = customOAuth2UserService;
        this.correlationIdFilter = correlationIdFilter;
        this.authUserArgumentResolver = authUserArgumentResolver;
        this.allowedOrigins = allowedOrigins;
    }

    // ArgumentResolver 등록
    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(authUserArgumentResolver);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .addFilterBefore(correlationIdFilter, UsernamePasswordAuthenticationFilter.class)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // csrf 개발환경에서는 모든 url 허가
                .csrf(csrf -> csrf.disable())

                // 인가(Authorization) 설정 - 모든 요청 허용 (테스트용)
                .authorizeHttpRequests(authz -> authz.anyRequest().permitAll())

                // API 요청은 인증 필요 시 401을 반환(리다이렉트 방지)
                .exceptionHandling(ex -> ex
                        .defaultAuthenticationEntryPointFor(
                                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                                new AntPathRequestMatcher("/api/**")
                        )
                );
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