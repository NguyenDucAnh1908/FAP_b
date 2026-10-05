package com.fap.common.config;

import com.fap.common.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

	private final JwtAuthenticationFilter jwtAuthenticationFilter;

	public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
		this.jwtAuthenticationFilter = jwtAuthenticationFilter;
	}

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http, AuthenticationProvider authenticationProvider) throws Exception {
		return http
				.csrf(AbstractHttpConfigurer::disable)
				.cors(Customizer.withDefaults())
				.headers(headers -> headers
						.contentTypeOptions(Customizer.withDefaults())
						.frameOptions(frame -> frame.deny())
						.referrerPolicy(referrer -> referrer.policy(ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
						.httpStrictTransportSecurity(hsts -> hsts
								.includeSubDomains(false)
								.preload(false)
								.maxAgeInSeconds(31_536_000)))
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authenticationProvider(authenticationProvider)
				.authorizeHttpRequests(requests -> requests
						.requestMatchers(
								"/actuator/health/**",
								"/v3/api-docs/**",
								"/swagger-ui/**",
								"/swagger-ui.html",
								"/api/v1/auth/**")
						.permitAll()
						// prometheus/info carry operational detail (endpoint list, build info, internal
						// metric names). Health stays public for probes; everything else under /actuator
						// must be authenticated so it is never scrape-able anonymously. A stricter
						// deployment can additionally bind actuator to a separate management port.
						.requestMatchers("/actuator/**").authenticated()
						.requestMatchers("/api/v1/**").authenticated()
						.anyRequest().permitAll())
				.exceptionHandling(exceptions -> exceptions.defaultAuthenticationEntryPointFor(
						new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
						new AntPathRequestMatcher("/actuator/**")))
				.httpBasic(AbstractHttpConfigurer::disable)
				.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
				.build();
	}

	@Bean
	AuthenticationProvider authenticationProvider(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
		DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
		provider.setUserDetailsService(userDetailsService);
		provider.setPasswordEncoder(passwordEncoder);
		return provider;
	}

	@Bean
	AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
		return configuration.getAuthenticationManager();
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
}
