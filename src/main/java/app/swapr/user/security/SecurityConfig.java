package app.swapr.user.security;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration(proxyBeanMethods = false)
class SecurityConfig {

  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http, SwaprSecurityProperties properties) throws Exception {
    AuthenticationEntryPoint entryPoint = problemEntryPoint();
    AccessDeniedHandler deniedHandler = problemAccessDeniedHandler();
    http
        .csrf(csrf -> csrf.disable())
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .cors(cors -> cors.configurationSource(corsConfigurationSource(properties.corsAllowedOrigins())))
        .addFilterBefore(new InternalTokenFilter(properties.internalToken()), BearerTokenAuthenticationFilter.class)
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/actuator/health", "/actuator/health/**", "/v3/api-docs", "/v3/api-docs/**")
            .permitAll()
            .requestMatchers("/internal/**").hasRole("INTERNAL")
            .requestMatchers("/v1/**").authenticated()
            .anyRequest().denyAll())
        .oauth2ResourceServer(oauth2 -> oauth2
            .jwt(jwt -> { })
            .authenticationEntryPoint(entryPoint)
            .accessDeniedHandler(deniedHandler))
        .exceptionHandling(exceptions -> exceptions
            .authenticationEntryPoint(entryPoint)
            .accessDeniedHandler(deniedHandler));
    return http.build();
  }

  private static AuthenticationEntryPoint problemEntryPoint() {
    BearerTokenAuthenticationEntryPoint bearer = new BearerTokenAuthenticationEntryPoint();
    return (request, response, exception) -> {
      bearer.commence(request, response, exception);
      ProblemResponses.write(response, HttpStatus.UNAUTHORIZED, "Authentication is required.",
          request.getRequestURI());
    };
  }

  private static AccessDeniedHandler problemAccessDeniedHandler() {
    BearerTokenAccessDeniedHandler bearer = new BearerTokenAccessDeniedHandler();
    return (request, response, exception) -> {
      bearer.handle(request, response, exception);
      ProblemResponses.write(response, HttpStatus.FORBIDDEN, "Access is denied.", request.getRequestURI());
    };
  }

  private static CorsConfigurationSource corsConfigurationSource(List<String> allowedOrigins) {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(allowedOrigins);
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
    config.setMaxAge(3600L);
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/v1/**", config);
    return source;
  }
}
