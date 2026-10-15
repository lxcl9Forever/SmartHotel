package com.smarthotel.config;

import com.smarthotel.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import jakarta.servlet.DispatcherType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(
                                "/api/v1/auth/**"
                        ).permitAll()

                        .requestMatchers("/api/v1/test/**")
                        .hasRole("ADMIN")

                        .requestMatchers(HttpMethod.POST, "/api/v1/payment/notify")
                        .permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/payment/create")
                        .hasRole("CUSTOMER")
                        .requestMatchers(HttpMethod.POST, "/api/v1/payment/query")
                        .hasRole("CUSTOMER")

                        .requestMatchers("/api/v1/admin/**")
                        .hasRole("ADMIN")

                        .requestMatchers("/api/v1/staff/**")
                        .hasAnyRole("ADMIN", "STAFF")

                        .requestMatchers("/api/v1/customer/**")
                        .hasRole("CUSTOMER")

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/room-types/**"
                        ).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/hotel", "/api/v1/hotel/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/daily-deals").permitAll()

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/bookings"
                        )
                        .hasRole("CUSTOMER")

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/bookings/**"
                        )
                        .hasRole("CUSTOMER")

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/bookings/*/cancel"
                        )
                        .hasRole("CUSTOMER")

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/bookings/*/mock-pay"
                        )
                        .hasRole("CUSTOMER")

                        .requestMatchers(
                                "/api/v1/customer/refund/**"
                        ).hasRole("CUSTOMER")

                        .anyRequest()
                        .authenticated()
                )

                .formLogin(form -> form.disable())

                .httpBasic(basic -> basic.disable())

                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
