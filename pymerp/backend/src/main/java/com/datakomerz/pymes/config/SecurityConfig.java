package com.datakomerz.pymes.config;

import com.datakomerz.pymes.security.AppUserDetailsService;
import com.datakomerz.pymes.security.jwt.JwtAuthenticationFilter;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
@Configuration
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

  private final AppProperties appProperties;
  private final JwtAuthenticationFilter jwtAuthenticationFilter;
  private final AppUserDetailsService userDetailsService;

  public SecurityConfig(AppProperties appProperties,
                        JwtAuthenticationFilter jwtAuthenticationFilter,
                        AppUserDetailsService userDetailsService) {
    this.appProperties = appProperties;
    this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    this.userDetailsService = userDetailsService;
  }

  @Bean
  SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.csrf(csrf -> csrf.disable())
      .cors(Customizer.withDefaults())
      .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

    boolean oidcEnabled = appProperties.getSecurity().getJwt().isOidcEnabled();

    if (oidcEnabled) {
      // resource server mode (Keycloak/Auth0) - use standard JWT converter for roles claim 'roles'
      http.authorizeHttpRequests(auth -> auth
          // Public actuator endpoints (health checks for load balancers)
          .requestMatchers("/actuator/health", "/actuator/info").permitAll()
          // Protected actuator endpoints (requires ACTUATOR_ADMIN role)
          .requestMatchers("/actuator/**").hasRole("ACTUATOR_ADMIN")
          // Public authentication endpoints
          .requestMatchers("/api/v1/auth/**", "/api/v1/requests/**", "/webhooks/billing").permitAll()
          .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
          .anyRequest().authenticated()
        )
        .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));
    } else {
      // default: internal JWT filter + DAO auth provider
      http.authorizeHttpRequests(auth -> auth
          // Public actuator endpoints (health checks for load balancers)
          .requestMatchers("/actuator/health", "/actuator/info").permitAll()
          // Protected actuator endpoints (requires ACTUATOR_ADMIN role)
          .requestMatchers("/actuator/**").hasRole("ACTUATOR_ADMIN")
          // Public authentication endpoints
          .requestMatchers("/api/v1/auth/**", "/api/v1/requests/**", "/webhooks/billing").permitAll()
          .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
          .anyRequest().authenticated()
        )
        .authenticationProvider(authenticationProvider())
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
    }
    return http.build();
  }

  @Bean
  public DaoAuthenticationProvider authenticationProvider() {
    DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
    provider.setUserDetailsService(userDetailsService);
    provider.setPasswordEncoder(passwordEncoder());
    return provider;
  }

  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
    return configuration.getAuthenticationManager();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration cfg = new CorsConfiguration();
    var cors = appProperties.getCors();
    var origins = cors.getAllowedOrigins();
    var originPatterns = cors.getAllowedOriginPatterns();
    boolean hasOrigins = origins != null && !origins.isEmpty();
    boolean hasPatterns = originPatterns != null && !originPatterns.isEmpty();
    
    // SECURITY: CORS must be explicitly configured - no wildcard fallback
    if (!hasOrigins && !hasPatterns) {
      throw new IllegalStateException(
        "CORS configuration is required. " +
        "Please set app.cors.allowed-origins or app.cors.allowed-origin-patterns in application.yml. " +
        "Wildcard (*) is not allowed for security reasons."
      );
    }
    
    if (hasOrigins) {
      cfg.setAllowedOrigins(origins);
    }
    if (hasPatterns) {
      cfg.setAllowedOriginPatterns(originPatterns);
    }
    
    cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
    cfg.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With", "X-Company-Id"));
    cfg.setAllowCredentials(true);
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", cfg);
    return source;
  }

  /**
   * JwtAuthenticationConverter that extracts roles from the claim `realm_access.roles` and
   * converts them to authorities with prefix `ROLE_`.
   */
  @Bean
  public JwtAuthenticationConverter jwtAuthenticationConverter() {
    JwtAuthenticationConverter jwtConverter = new JwtAuthenticationConverter();
    jwtConverter.setJwtGrantedAuthoritiesConverter(jwt -> {
      // delegate to our utility which works with a claims map
      Map<String, Object> claims = jwt.getClaims();
      return com.datakomerz.pymes.security.OidcRoleMapper.mapRolesFromClaims(claims);
    });
    return jwtConverter;
  }
}
