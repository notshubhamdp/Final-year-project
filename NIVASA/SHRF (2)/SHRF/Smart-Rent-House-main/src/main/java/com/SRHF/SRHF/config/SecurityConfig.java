package com.SRHF.SRHF.config;

import com.SRHF.SRHF.service.CustomUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final CustomAuthenticationSuccessHandler successHandler;

    public SecurityConfig(CustomUserDetailsService userDetailsService, 
                         CustomAuthenticationSuccessHandler successHandler) {
        this.userDetailsService = userDetailsService;
        this.successHandler = successHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(req -> req
                    .requestMatchers("/", "/home", "/register/**", "/login", "/forgot-password/**", "/css/**", "/js/**", "/images/**", "/uploads/**", "/terms", "/terms.html", "/privacy-policy", "/privacy-policy.html", "/contact-us", "/contact-us.html", "/faq", "/faq.html", "/api/chatbot/**").permitAll()
                    .requestMatchers("/payment/test-email").permitAll()
                    .requestMatchers("/payment/webhook").permitAll()
                    .requestMatchers("/role-selection/**").authenticated()
                    .requestMatchers("/landlord/**").authenticated()
                    .requestMatchers("/landlord-dashboard").authenticated()
                    .requestMatchers("/tenant-dashboard","/profile/**","/favorites").authenticated()
                    .requestMatchers("/messages/**").authenticated()
                    .requestMatchers("/visits/**").authenticated()
                    .requestMatchers("/payment/download-receipt/**").authenticated()
                    .requestMatchers("/payment/**").authenticated()
                    .requestMatchers("/admin-dashboard","/admin/**").hasRole("ADMIN")
                    .anyRequest().authenticated()
                )
                .csrf(csrf -> csrf
                    .ignoringRequestMatchers("/payment/**")
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        // our login form will submit the email field; treat it as the username
                        .usernameParameter("email")
                        .successHandler(successHandler)
                        .permitAll()
                )
                .logout(logout -> logout
                    .logoutUrl("/perform_logout")
                    .invalidateHttpSession(true)
                    .clearAuthentication(true)
                    .deleteCookies("JSESSIONID")
                    .logoutSuccessUrl("/login?logout")
                    .permitAll()
                )
                .userDetailsService(userDetailsService);

        return http.build();
    }

}
