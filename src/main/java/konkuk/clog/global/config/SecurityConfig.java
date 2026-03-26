package konkuk.clog.global.config;

import java.util.List;
import konkuk.clog.domain.user.repository.UserRepository;
import konkuk.clog.global.security.JwtAuthenticationFilter;
import konkuk.clog.global.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * JWT 기반 stateless 보안, CORS(허용 출처 화이트리스트), 보안 헤더, 공개 API 정책.
 */
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, SecurityProperties securityProperties)
            throws Exception {
        JwtAuthenticationFilter jwtFilter =
                new JwtAuthenticationFilter(jwtTokenProvider, userRepository);

        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource(securityProperties)))
                .sessionManagement(sess ->
                        sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers -> headers
                        .frameOptions(frame -> frame.deny())
                        .contentTypeOptions(Customizer.withDefaults()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/blogs/published").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/blogs/users/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/blogs/*").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/blogs/*/view").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/comments/blog/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/comments").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/quizzes/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/quizzes").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/quizzes/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/quizzes/**").authenticated()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Bearer JWT 만 사용하므로 {@code Allow-Credentials: false} 로 CSRF/CORS 결합 위험을 줄인다.
     * 허용 Origin 은 {@link SecurityProperties#corsAllowedOrigins} 로만 제한(와일드카드 https://* 금지).
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(SecurityProperties securityProperties) {
        CorsConfiguration config = new CorsConfiguration();
        List<String> origins = securityProperties.parsedCorsOrigins();
        if (origins.isEmpty()) {
            origins = List.of("http://localhost:3000", "http://localhost:5173");
        }
        config.setAllowedOrigins(origins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization"));
        config.setAllowCredentials(false);
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
